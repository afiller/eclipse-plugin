package com.vaadin.integration.eclipse.wizards;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.configuration.VaadinProjectCreationDataModelProvider;
import com.vaadin.integration.eclipse.util.ErrorUtil;

/**
 * Vaadin top level project creation wizard.
 * 
 * Note that Vaadin projects can also be created through the normal
 * "Dynamic Web Project" wizard by adding the Vaadin facet there or by adding
 * the Vaadin facet to an already existing project.
 * 
 * This is really just a customized and pre-configured version of the standard
 * dynamic web project creation wizard.
 */
public abstract class VaadinProjectWizard extends WebProjectWizard {
    public VaadinProjectWizard(IDataModel model) {
        super(model);
        setWindowTitle(getProjectTypeTitle());
    }

    public VaadinProjectWizard() {
        super();
        setWindowTitle(getProjectTypeTitle());
    }

    protected abstract String getProjectTypeTitle();

    @Override
    protected IDataModel createDataModel() {
        try {
            ProjectFacetsManager
                    .getProjectFacet(VaadinFacetUtils.VAADIN_FACET_ID);
            return DataModelFactory
                    .createDataModel(new VaadinProjectCreationDataModelProvider());
        } catch (Exception e) {
            ErrorUtil.handleBackgroundException(e);
            return null;
        }
    }

    @Override
    protected IWizardPage createFirstPage() {
        return new VaadinProjectFirstPage(model, "first.page"); //$NON-NLS-1$
    }

    @Override
    protected void setRuntimeAndDefaultFacets(IRuntime runtime) {
        super.setRuntimeAndDefaultFacets(runtime);

        // select the Vaadin preset configuration by default
        final IFacetedProjectWorkingCopy dm = getFacetedProjectWorkingCopy();
        dm.setSelectedPreset(getDefaultPreset());
    }

    protected abstract String getDefaultPreset();

}
