/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.viewer.wicket.ui.components.scalars.reference;

import java.util.List;

import com.google.common.collect.Lists;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Settings;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.mgr.AdapterManager.ConcurrencyChecking;
import org.apache.isis.core.metamodel.facets.object.autocomplete.AutoCompleteFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.viewer.wicket.model.isis.WicketViewerSettings;
import org.apache.isis.viewer.wicket.model.mementos.ObjectAdapterMemento;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.model.models.EntityModelForReference;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.ComponentFactory;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelAbstract2;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelSelect2Abstract;
import org.apache.isis.viewer.wicket.ui.components.widgets.bootstrap.FormGroup;
import org.apache.isis.viewer.wicket.ui.components.widgets.entitysimplelink.EntityLinkSimplePanel;
import org.apache.isis.viewer.wicket.ui.components.widgets.select2.Select2;
import org.apache.isis.viewer.wicket.ui.components.widgets.select2.providers.ObjectAdapterMementoProviderForReferenceChoices;
import org.apache.isis.viewer.wicket.ui.components.widgets.select2.providers.ObjectAdapterMementoProviderForReferenceObjectAutoComplete;
import org.apache.isis.viewer.wicket.ui.components.widgets.select2.providers.ObjectAdapterMementoProviderForReferenceParamOrPropertyAutoComplete;
import org.apache.isis.viewer.wicket.ui.util.Components;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

/**
 * Panel for rendering scalars which of are of reference type (as opposed to
 * value types).
 */
public class ReferencePanel extends ScalarPanelSelect2Abstract {

    private static final long serialVersionUID = 1L;

    private static final String ID_AUTO_COMPLETE = "autoComplete";
    private static final String ID_ENTITY_ICON_TITLE = "entityIconAndTitle";


    /**
     * Determines the behaviour of dependent choices for the dependent; either to autoselect the first available choice, or to select none.
     */
    private static final String KEY_DISABLE_DEPENDENT_CHOICE_AUTO_SELECTION = "isis.viewer.wicket.disableDependentChoiceAutoSelection";

    private EntityLinkSelect2Panel entityLink;

    private EntityLinkSimplePanel entitySimpleLink;


    public ReferencePanel(final String id, final ScalarModel scalarModel) {
        super(id, scalarModel);
    }


    Select2 getSelect2() {
        return select2;
    }

    // //////////////////////////////////////

    // First called as a side-effect of {@link #beforeRender()}
    @Override
    protected Component createComponentForCompact() {

        final ScalarModel scalarModel = getModel();
        final String name = scalarModel.getName();
        
        entitySimpleLink = (EntityLinkSimplePanel) getComponentFactoryRegistry().createComponent(ComponentType.ENTITY_LINK, getModel());
        
        entitySimpleLink.setOutputMarkupId(true);
        entitySimpleLink.setLabel(Model.of(name));
        
        final WebMarkupContainer labelIfCompact = new WebMarkupContainer(ID_SCALAR_IF_COMPACT);
        labelIfCompact.add(entitySimpleLink);

        return labelIfCompact;
    }

    // First called as a side-effect of {@link #beforeRender()}
    @Override
    protected FormGroup createComponentForRegular() {

        entityLink = new EntityLinkSelect2Panel(ComponentType.ENTITY_LINK.getWicketId(), this);

        entityLink.setRequired(getModel().isRequired());
        this.select2 = createSelect2AndSemantics();
        entityLink.addOrReplace(select2.component());

        syncWithInput();

        entityLink.setOutputMarkupId(true);

        FormComponent<?> formComponent = this.entityLink;

        return createFormGroup(formComponent);
    }


    private Select2 createSelect2AndSemantics() {

        final Select2 select2 = createSelect2(ID_AUTO_COMPLETE);


        final Settings settings = select2.getSettings();

        // one of these three case should be true
        // (as per the isEditableWithEitherAutoCompleteOrChoices() guard above)
        if(getModel().hasChoices()) {

            settings.setPlaceholder(getModel().getName());

        } else if(getModel().hasAutoComplete()) {

            final int minLength = getModel().getAutoCompleteMinLength();
            settings.setMinimumInputLength(minLength);
            settings.setPlaceholder(getModel().getName());

        } else if(hasObjectAutoComplete()) {
            final ObjectSpecification typeOfSpecification = getModel().getTypeOfSpecification();
            final AutoCompleteFacet autoCompleteFacet = typeOfSpecification.getFacet(AutoCompleteFacet.class);
            final int minLength = autoCompleteFacet.getMinLength();
            settings.setMinimumInputLength(minLength);
        }

        return select2;
    }



    // //////////////////////////////////////

    @Override
    protected InlinePromptConfig getInlinePromptConfig() {
        return InlinePromptConfig.supportedAndHide(select2.component());
    }

    @Override
    protected IModel<String> obtainInlinePromptModel() {
        final IModel<ObjectAdapterMemento> model = select2.getModel();
        return new IModel<String>() {
            @Override
            public String getObject() {
                final ObjectAdapterMemento oam = model.getObject();
                if(oam == null) {
                    return null;
                }
                ObjectAdapter objectAdapter = oam
                        .getObjectAdapter(ConcurrencyChecking.NO_CHECK, getPersistenceSession(),
                                getSpecificationLoader());
                return objectAdapter != null ? objectAdapter.titleString(null) : null;
            }

            @Override
            public void setObject(final String s) {
                // ignore
            }

            @Override
            public void detach() {
                // ignore
            }
        };
    }





    // //////////////////////////////////////
    // onBeforeRender*
    // //////////////////////////////////////

    @Override
    protected void onInitializeWhenEnabled() {
        super.onInitializeWhenEnabled();
        if(getRendering() == Rendering.COMPACT) {
            return;
        }

        entityLink.setEnabled(true);
        syncWithInput();
    }

    @Override
    protected void onInitializeWhenViewMode() {
        super.onInitializeWhenViewMode();
        if(getRendering() == Rendering.COMPACT) {
            return;
        }

        entityLink.setEnabled(false);
        syncWithInput();
    }

    @Override
    protected void onInitializeWhenDisabled(final String disableReason) {
        super.onInitializeWhenDisabled(disableReason);
        if(getRendering() == Rendering.COMPACT) {
            return;
        }

        syncWithInput();
        final EntityModel entityLinkModel = (EntityModel) entityLink.getModel();
        entityLinkModel.toViewMode();
        entityLink.setEnabled(false);
        entityLink.add(new AttributeModifier("title", Model.of(disableReason)));
    }

    @Override
    protected void onDisabled(final String disableReason, final AjaxRequestTarget target) {
        super.onDisabled(disableReason, target);
        if(getRendering() == Rendering.COMPACT) {
            return;
        }

        entityLink.setEnabled(false);
        entityLink.add(new AttributeModifier("title", Model.of(disableReason)));
    }

    @Override
    protected void onEnabled(final AjaxRequestTarget target) {
        super.onEnabled(target);
        if(getRendering() == Rendering.COMPACT) {
            return;
        }

        entityLink.setEnabled(true);
        entityLink.add(new AttributeModifier("title", Model.of("")));
    }



    // //////////////////////////////////////
    // syncWithInput
    // //////////////////////////////////////


    // called from onInitialize*
    // (was previous called by EntityLinkSelect2Panel in onBeforeRender, this responsibility now moved)
    private void syncWithInput() {
        final ObjectAdapter adapter = getModel().getPendingElseCurrentAdapter();

        // syncLinkWithInput
        final MarkupContainer componentForRegular = (MarkupContainer) getComponentForRegular();

        if(componentForRegular != null) {

            final EntityModelForReference entityModelForLink = new EntityModelForReference(getModel());

            entityModelForLink.setContextAdapterIfAny(getModel().getContextAdapterIfAny());
            entityModelForLink.setRenderingHint(getModel().getRenderingHint());

            final ComponentFactory componentFactory =
                    getComponentFactoryRegistry().findComponentFactory(ComponentType.ENTITY_ICON_AND_TITLE, entityModelForLink);
            final Component component = componentFactory.createComponent(ComponentType.ENTITY_ICON_AND_TITLE.getWicketId(), entityModelForLink);

            componentForRegular.addOrReplace(component);

            boolean inlinePrompt = scalarModel.isInlinePrompt(getDeploymentCategory());
            if(inlinePrompt) {
                // bit of a hack... allows us to suppress the title using CSS
                component.add(new CssClassAppender("inlinePrompt"));
            }

            if (adapter != null) {

                Components.permanentlyHide(componentForRegular, "entityTitleIfNull");

            } else {

                if(inlinePrompt) {
                    Components.permanentlyHide(componentForRegular, "entityTitleIfNull");
                } else {
                    componentForRegular.addOrReplace(new Label("entityTitleIfNull", "(none)"));
                }
            }

        }


        // syncLinkWithInputIfAutoCompleteOrChoices
        if(isEditableWithEitherAutoCompleteOrChoices()) {

            if(select2 == null) {
                throw new IllegalStateException("select2 should be created already");
            } else {
                //
                // the select2Choice already exists, so the widget has been rendered before.  If it is
                // being re-rendered now, it may be because some other property/parameter was invalid.
                // when the form was submitted, the selected object (its oid as a string) would have
                // been saved as rawInput.  If the property/parameter had been valid, then this rawInput
                // would be correctly converted and processed by the select2Choice's choiceProvider.  However,
                // an invalid property/parameter means that the webpage is re-rendered in another request,
                // and the rawInput can no longer be interpreted.  The net result is that the field appears
                // with no input.
                //
                // The fix is therefore (I think) simply to clear any rawInput, so that the select2Choice
                // renders its state from its model.
                //
                // see: FormComponent#getInputAsArray()
                // see: Select2Choice#renderInitializationScript()
                //
                select2.clearInput();
            }

            if(getComponentForRegular() != null) {
                Components.permanentlyHide((MarkupContainer)getComponentForRegular(), ID_ENTITY_ICON_TITLE);
                Components.permanentlyHide(componentForRegular, "entityTitleIfNull");
            }



            // syncUsability
            if(select2 != null) {
                final boolean mutability = entityLink.isEnableAllowed() && !getModel().isViewMode();
                select2.setEnabled(mutability);
            }

            Components.permanentlyHide(entityLink, "entityLinkIfNull");
        } else {
            // this is horrid; adds a label to the id
            // should instead be a 'temporary hide'
            Components.permanentlyHide(entityLink, ID_AUTO_COMPLETE);
            // setSelect2(null); // this forces recreation next time around
        }
        
    }

    // //////////////////////////////////////
    // setProviderAndCurrAndPending
    // //////////////////////////////////////
    
    @Override
    protected ChoiceProvider<ObjectAdapterMemento> buildChoiceProvider(final ObjectAdapter[] argsIfAvailable) {

        if (getModel().hasChoices()) {
            List<ObjectAdapterMemento> choiceMementos = obtainChoiceMementos(argsIfAvailable);
            return new ObjectAdapterMementoProviderForReferenceChoices(getModel(), wicketViewerSettings, choiceMementos);
        }

        if(getModel().hasAutoComplete()) {
            return new ObjectAdapterMementoProviderForReferenceParamOrPropertyAutoComplete(getModel(), wicketViewerSettings);
        }

        return new ObjectAdapterMementoProviderForReferenceObjectAutoComplete(getModel(), wicketViewerSettings);
    }

    // called by setProviderAndCurrAndPending
    private List<ObjectAdapterMemento> obtainChoiceMementos(final ObjectAdapter[] argsIfAvailable) {
        final List<ObjectAdapter> choices = Lists.newArrayList();
        if(getModel().hasChoices()) {
            choices.addAll(getModel().getChoices(argsIfAvailable, getAuthenticationSession(), getDeploymentCategory()));
        }
        // take a copy (otherwise is only lazily evaluated)
        return Lists.newArrayList(Lists.transform(choices, ObjectAdapterMemento.Functions.fromAdapter()));
    }

    // called by setProviderAndCurrAndPending
    @Override
    protected void syncIfNull(final Select2 select2, final List<ObjectAdapterMemento> choiceMementos) {
        final ObjectAdapterMemento curr = select2.getModelObject();

        if(!getModel().isCollection()) {

            if(curr == null) {
                select2.getModel().setObject(null);
                getModel().setObject(null);
            }

        }
    }

    private boolean autoSelect() {
        final boolean disableAutoSelect = getConfiguration().getBoolean(KEY_DISABLE_DEPENDENT_CHOICE_AUTO_SELECTION, false);
        final boolean autoSelect = !disableAutoSelect;
        return autoSelect;
    }

    // //////////////////////////////////////
    // getInput, convertInput
    // //////////////////////////////////////
    
    // called by EntityLinkSelect2Panel
    String getInput() {
        final ObjectAdapter pendingElseCurrentAdapter = getModel().getPendingElseCurrentAdapter();
        return pendingElseCurrentAdapter != null? pendingElseCurrentAdapter.titleString(null): "(no object)";
    }

    // //////////////////////////////////////

    // called by EntityLinkSelect2Panel
    void convertInput() {
        if(isEditableWithEitherAutoCompleteOrChoices()) {

            // flush changes to pending
            ObjectAdapterMemento convertedInput =
                    select2.getConvertedInput();
            
            getModel().setPending(convertedInput);
            if(select2 != null) {
                select2.getModel().setObject(convertedInput);
            }
            
            final ObjectAdapter adapter = convertedInput!=null?convertedInput.getObjectAdapter(ConcurrencyChecking.NO_CHECK,
                    getPersistenceSession(), getSpecificationLoader()):null;
            getModel().setObject(adapter);
        }
    
        final ObjectAdapter pendingAdapter = getModel().getPendingAdapter();
        entityLink.setConvertedInput(pendingAdapter);
    }


    
    // //////////////////////////////////////

    @Override
    public void onUpdate(
            final AjaxRequestTarget target, final ScalarPanelAbstract2 scalarPanel) {

        super.onUpdate(target, scalarPanel);

        target.appendJavaScript(
                String.format("Wicket.Event.publish(Isis.Topic.CLOSE_SELECT2, '%s')", getMarkupId()));

    }


    // //////////////////////////////////////
    // helpers querying model state
    // //////////////////////////////////////

    // called from convertInput, syncWithInput
    private boolean isEditableWithEitherAutoCompleteOrChoices() {
        if(getModel().getRenderingHint().isInTable()) {
            return false;
        }
        // doesn't apply if not editable, either
        if(getModel().isViewMode()) {
            return false;
        }
        return getModel().hasChoices() || getModel().hasAutoComplete() || hasObjectAutoComplete();
    }

    // called by isEditableWithEitherAutoCompleteOrChoices
    private boolean hasObjectAutoComplete() {
        final ObjectSpecification typeOfSpecification = getModel().getTypeOfSpecification();
        final AutoCompleteFacet autoCompleteFacet = 
                (typeOfSpecification != null)? typeOfSpecification.getFacet(AutoCompleteFacet.class):null;
        return autoCompleteFacet != null;
    }


    @Override
    protected String getScalarPanelType() {
        return "referencePanel";
    }


    // //////////////////////////////////////

    @com.google.inject.Inject
    WicketViewerSettings wicketViewerSettings;

}


