package tnode;

import java.io.IOException;
import java.util.NavigableMap;

import exception.HBSException;

import main.HBaseShellPro;

import task.TaskBase;
import task.TaskBase.Level;
import utils.Utils;

public class TNodeFamily extends TNodeBase {
    private final NavigableMap<byte[], byte[]> familyMap;

    public TNodeFamily(TaskBase task, TNodeRow parent, String family, NavigableMap<byte[], byte[]> familyMap, boolean toOutput) {
        super(task, parent, family, Level.FAMILY, toOutput);

        this.familyMap = familyMap;
    }

    @Override
    protected String formatString() {
        return HBaseShellPro.format_family;
    }

    @Override
    public void output()
    throws IOException, HBSException {
        if (!outputted) {
            HBaseShellPro.increaseCount(HBaseShellPro.FAMILY);
        }

        super.output();
    }

    @Override
    protected void travelChildren()
    throws IOException, HBSException {
        for (byte[] bQualifier : familyMap.keySet()) {
            String qualifier = Utils.bytes2str(bQualifier);
            new TNodeQualifier(task, this, qualifier, familyMap.get(bQualifier), toOutput).handle();
        }
    }

    public static boolean isFileDataFamily(String family) {
        return family.equals("file") || family.equals("tmp");
    }
}
