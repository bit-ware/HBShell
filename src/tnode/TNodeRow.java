package tnode;

import static common.Common.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import main.HBaseShellPro;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.filter.FilterList.Operator;

import exception.HBSException;
import task.TaskBase;
import task.TaskBase.Level;
import utils.Utils;

public class TNodeRow extends TNodeBase {
    public final HTable  table;
    private final Result firstKVResult;

    private List<String> families         = null;
    private List<String> fileDataFamilies = null;

    public TNodeRow(TaskBase task, TNodeTable parent, HTable table, Result firstKVResult, boolean toOutput) {
        super(task, parent, Utils.resultGetRowKey(firstKVResult), Level.ROW, toOutput);

        this.table         = table;
        this.firstKVResult = firstKVResult;
    }

    @Override
    protected String formatString() {
        return HBaseShellPro.format_row;
    }

    @Override
    public void output()
    throws IOException, HBSException {
        if (!outputted) {
            HBaseShellPro.increaseCount(HBaseShellPro.ROW);
        }

        super.output();
    }

    @Override
    protected void travelChildren()
    throws IOException, HBSException {
        // get filtered families and fileDataFamilies
        List<String> allFamilies = Utils.getFamilies(table);
        this.families         = new ArrayList<String>();
        this.fileDataFamilies = new ArrayList<String>();

        for (String family : allFamilies) {
            if (task.isMatch(Level.FAMILY, family)) {
                families.add(family);

                if (TNodeFamily.isFileDataFamily(family)) {
                    fileDataFamilies.add(family);
                }
            }
        }

        // travel non-file-data
        // if file-data is in the same family, they will also be travelled
        travelChildrenNonFileData();

        // travel remained file-data
        for (String family : fileDataFamilies) {
            TNodeFamilyFileData familyFileData = getFamilyFileData(family, null);

            if (familyFileData != null) {
                familyFileData.handle();
            }
        }
    }

    private void travelChildrenNonFileData()
    throws IOException, HBSException {
        Get get = new Get(str2bytes(name));

        // filter family
        for (String family : families) {
            get.addFamily(str2bytes(family));
        }

        FilterList filterList = new FilterList(Operator.MUST_PASS_ALL);

        // filter file data qualifier (excluded)
        filterList.addFilter(getFileDataQualifierFilter(false));

        // filter qualifier
        CompareFilter qualifierPatternFilter = getQualifierPatternFilter();

        if (qualifierPatternFilter != null) {
            filterList.addFilter(qualifierPatternFilter);
        }

        // filter value
        CompareFilter valuePatternFilter = getValuePatternFilter();

        if (valuePatternFilter != null) {
            filterList.addFilter(valuePatternFilter);
        }

        get.setFilter(filterList);

        // get result
        Result result = table.get(get);

        if (result.isEmpty()) {
            return;
        }

        NavigableMap<byte[], NavigableMap<byte[], byte[]> > map = result.getNoVersionMap();

        for (byte[] bFamily : map.keySet()) {
            String family = Utils.bytes2str(bFamily);

            // if no result, hbase will get all families
            // so filter family again
            if (!families.contains(family)) {
                continue;
            }

            TNodeFamily familyNode = new TNodeFamily(task, this, family, map.get(bFamily), toOutput);

            familyNode.handle();

            if (TNodeFamily.isFileDataFamily(family)) {
                TNodeFamilyFileData familyFileData = getFamilyFileData(family, familyNode);

                if (familyFileData != null) {
                    familyFileData.handle();
                }

                fileDataFamilies.remove(family);
            }
        }
    }

    public TNodeFamilyFileData getFamilyFileData(String family, TNodeFamily familyNode)
    throws IOException {
        if (!HBaseShellPro.travelRowFBlockFamilies) {
            return null;
        }

        // if firstKVResult is file data, then use it directly instead of getting it once more
        TNodeFamilyFileData familyFileData = getFamilyFileDataUsingFirstKVResult(family, familyNode);

        if (familyFileData != null) {
            return familyFileData;
        }

        Get get = new Get(str2bytes(name));

        // filter family
        get.addFamily(str2bytes(family));

        FilterList filterList = new FilterList(Operator.MUST_PASS_ALL);

        // filter file data qualifier (included)
        // this filter must be previous to FirstKeyOnlyFilter
        // the order is important
        filterList.addFilter(getFileDataQualifierFilter(true));

        // do not filter qualifier & value, filter them in family level

        // get only first kv
        filterList.addFilter(new FirstKeyOnlyFilter());

        get.setFilter(filterList);

        // get result
        Result result = table.get(get);

        if (!result.isEmpty()) {
            NavigableMap<byte[], byte[]> familyMap = result.getFamilyMap(str2bytes(family));

            byte[] bFirstKey = familyMap.firstKey();
            String firstFileDataQualifier = Utils.bytes2str(bFirstKey);
            byte[] firstFileDataBValue = familyMap.get(bFirstKey);

            return new TNodeFamilyFileData(task, this, family, table, firstFileDataQualifier, firstFileDataBValue, familyNode, toOutput);
        }

        return null;
    }

    private TNodeFamilyFileData getFamilyFileDataUsingFirstKVResult(String family, TNodeFamily familyNode) {
        NavigableMap<byte[], byte[]> familyMap = firstKVResult.getFamilyMap(str2bytes(family));

        if (!familyMap.isEmpty()) {
            byte[] bFirstKey = familyMap.firstKey();
            String qualifier = Utils.bytes2str(bFirstKey);

            if (Utils.isMatch(qualifier, FILE_DATA_QUALIFIER_PATTERN)) {
                byte[] bValue = familyMap.get(bFirstKey);
                return new TNodeFamilyFileData(task, this, family, table, qualifier, bValue, familyNode, toOutput);
            }
        }

        return null;
    }
}
