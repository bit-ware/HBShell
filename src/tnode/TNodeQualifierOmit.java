package tnode;

import java.io.IOException;

import exception.HBSException;

import main.HBaseShellPro;

import task.TaskBase;
import task.TaskBase.Level;

public class TNodeQualifierOmit extends TNodeBase {
    private static final String NAME = "...";

    private final long firstIndex;
    private final long lastIndex;

    public TNodeQualifierOmit(TaskBase task, TNodeFamily parent, long firstIndex, long lastIndex, boolean toOutput) {
        super(task, parent, NAME, Level.QUALIFIER, toOutput);

        this.firstIndex = firstIndex;
        this.lastIndex  = lastIndex;
    }

    @Override
    protected String formatString() {
        return HBaseShellPro.format_qualifierOmit;
    }

    @Override
    public void handle()
    throws IOException, HBSException {
        long count = lastIndex - firstIndex - 1;

        HBaseShellPro.increaseCount(HBaseShellPro.QUALIFIER, count);
        HBaseShellPro.increaseCount(HBaseShellPro.VALUE, count);

        output();
    }

    @Override
    protected void travelChildren() {
        // do nothing
    }
}
