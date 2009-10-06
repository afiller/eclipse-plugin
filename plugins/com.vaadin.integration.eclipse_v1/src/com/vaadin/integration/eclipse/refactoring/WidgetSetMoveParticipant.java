package com.vaadin.integration.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

public class WidgetSetMoveParticipant extends
        org.eclipse.ltk.core.refactoring.participants.MoveParticipant {

    private WidgetSetRefactorer refactorer = new WidgetSetRefactorer();

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm,
            CheckConditionsContext context) throws OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        return refactorer.createChange(pm);
    }

    @Override
    public String getName() {
        return "Vaadin Widgetset Move Participant";
    }

    @Override
    protected boolean initialize(Object element) {
        // Do not update references if not requested
        if (!getArguments().getUpdateReferences()) {
            return false;
        }

        return refactorer.initializeMove(element, getArguments()
                .getDestination());
    }

}
