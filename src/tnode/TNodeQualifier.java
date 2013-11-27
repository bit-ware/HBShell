package tnode;

import java.io.IOException;

import exception.HBSException;

import main.HBaseShellPro;

import task.TaskBase;
import task.TaskBase.Level;

public class TNodeQualifier extends TNodeBase {
    private final byte[] bValue;

    public TNodeQualifier(TaskBase task, TNodeFamily parent, String qualifier, byte[] bValue, boolean toOutput) {
        super(task, parent, qualifier, Level.QUALIFIER, toOutput);

        this.bValue = bValue;
    }

    @Override
    protected String formatString() {
        return HBaseShellPro.format_qualifier;
    }

    @Override
    public void output()
    throws IOException, HBSException {
        if (!outputted) {
            HBaseShellPro.increaseCount(HBaseShellPro.QUALIFIER);
        }

        super.output();
    }

    @Override
    protected void travelChildren()
    throws IOException, HBSException {
        new TNodeValue(task, this, bValue, toOutput).handle();
    }
}
