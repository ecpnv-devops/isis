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

package org.apache.isis.viewer.wicket.ui.components.scalars;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.PromptStyle;
import org.apache.isis.applib.services.metamodel.MetaModelService2;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.mgr.AdapterManager;
import org.apache.isis.core.metamodel.adapter.version.ConcurrencyException;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.core.metamodel.facets.all.named.NamedFacet;
import org.apache.isis.core.metamodel.facets.members.cssclass.CssClassFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.labelat.LabelAtFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.wicket.model.links.LinkAndLabel;
import org.apache.isis.viewer.wicket.model.mementos.ActionParameterMemento;
import org.apache.isis.viewer.wicket.model.models.ActionArgumentModel;
import org.apache.isis.viewer.wicket.model.models.ActionModel;
import org.apache.isis.viewer.wicket.model.models.ActionPrompt;
import org.apache.isis.viewer.wicket.model.models.ActionPromptProvider;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.model.models.InlinePromptContext;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.components.actionmenu.entityactions.AdditionalLinksPanel;
import org.apache.isis.viewer.wicket.ui.components.actionmenu.entityactions.LinkAndLabelUtil;
import org.apache.isis.viewer.wicket.ui.components.property.PropertyEditFormPanel;
import org.apache.isis.viewer.wicket.ui.components.property.PropertyEditPanel;
import org.apache.isis.viewer.wicket.ui.components.propertyheader.PropertyEditPromptHeaderPanel;
import org.apache.isis.viewer.wicket.ui.components.scalars.isisapplib.IsisBlobOrClobPanelAbstract;
import org.apache.isis.viewer.wicket.ui.components.scalars.primitive.BooleanPanel;
import org.apache.isis.viewer.wicket.ui.components.scalars.reference.ReferencePanel;
import org.apache.isis.viewer.wicket.ui.components.scalars.valuechoices.ValueChoicesSelect2Panel;
import org.apache.isis.viewer.wicket.ui.components.widgets.linkandlabel.ActionLink;
import org.apache.isis.viewer.wicket.ui.pages.entity.EntityPage;
import org.apache.isis.viewer.wicket.ui.panels.PanelAbstract;
import org.apache.isis.viewer.wicket.ui.util.Components;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

public abstract class ScalarPanelAbstract2 extends PanelAbstract<ScalarModel> implements ScalarModelSubscriber2 {

    private static final long serialVersionUID = 1L;

    protected static final String ID_SCALAR_TYPE_CONTAINER = "scalarTypeContainer";

    protected static final String ID_SCALAR_IF_COMPACT = "scalarIfCompact";
    protected static final String ID_SCALAR_IF_REGULAR = "scalarIfRegular";
    protected static final String ID_SCALAR_NAME = "scalarName";
    protected static final String ID_SCALAR_VALUE = "scalarValue";



    /**
     * as per {@link #inlinePromptLink}
     */
    protected static final String ID_SCALAR_VALUE_INLINE_PROMPT_LINK = "scalarValueInlinePromptLink";
    protected static final String ID_SCALAR_VALUE_INLINE_PROMPT_LABEL = "scalarValueInlinePromptLabel";

    /**
     * as per {@link #scalarIfRegularInlinePromptForm}.
     */
    public static final String ID_SCALAR_IF_REGULAR_INLINE_PROMPT_FORM = "scalarIfRegularInlinePromptForm";


    private static final String ID_EDIT_PROPERTY = "editProperty";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_ASSOCIATED_ACTION_LINKS_BELOW = "associatedActionLinksBelow";
    private static final String ID_ASSOCIATED_ACTION_LINKS_RIGHT = "associatedActionLinksRight";

    public enum Repaint {
        ENTIRE_FORM,
        PARAM_ONLY,
        NOTHING
    }

    /**
     *
     * @param actionModel - the action being invoked
     * @param paramNumUpdated - the # of the param that has just been updated by the user
     * @param paramNumToPossiblyUpdate - the # of the param to be updated if necessary (will be &gt; paramNumUpdated)
     *
     * @param target - in case there's more to be repainted...
     *
     * @return - true if changed as a result of these pending arguments.
     */
    public Repaint updateIfNecessary(
            final ActionModel actionModel,
            final int paramNumUpdated,
            final int paramNumToPossiblyUpdate,
            final AjaxRequestTarget target) {

        final ObjectAction action = actionModel.getActionMemento().getAction(getSpecificationLoader());
        final ObjectAdapter[] pendingArguments = actionModel.getArgumentsAsArray();


        // could almost certainly simplify this... (used by visibility and usability checks)
        final ObjectActionParameter actionParameter = action.getParameters().get(paramNumToPossiblyUpdate);
        final ObjectAdapter targetAdapter = actionModel.getTargetAdapter();
        final ObjectAdapter realTargetAdapter = action.realTargetAdapter(targetAdapter);

        // check visibility
        final Consent visibilityConsent = actionParameter.isVisible(realTargetAdapter, pendingArguments, InteractionInitiatedBy.USER);

        final boolean visibilityBefore = isVisible();
        final boolean visibilityAfter = visibilityConsent.isAllowed();
        setVisible(visibilityAfter);


        // check usability
        final Consent usabilityConsent = actionParameter.isUsable(realTargetAdapter, pendingArguments, InteractionInitiatedBy.USER);

        final boolean usabilityBefore = isEnabled();
        final boolean usabilityAfter = usabilityConsent.isAllowed();
        if(usabilityAfter) {
            onEnabled(target);
        } else {
            onDisabled(usabilityConsent.getReason(), target);
        }


        // even if now invisible or unusable, we recalculate args and ensure compatible
        // (else can hit complicated edge cases with stale data when next re-enable/make visible)
        final ScalarModel model = getModel();
        ObjectAdapter defaultIfAny = model.getKind()
                .getDefault(scalarModel, pendingArguments, paramNumUpdated,
                        getAuthenticationSession(), getDeploymentCategory());

        final ActionParameterMemento apm = new ActionParameterMemento(actionParameter);
        final ActionArgumentModel actionArgumentModel = actionModel.getArgumentModel(apm);

        final ObjectAdapter pendingArg = pendingArguments[paramNumToPossiblyUpdate];

        if (defaultIfAny != null) {
            scalarModel.setObject(defaultIfAny);

            scalarModel.setPendingAdapter(defaultIfAny);
            actionArgumentModel.setObject(defaultIfAny);
        } else {

            if(pendingArg != null & scalarModel.hasChoices()) {
                // make sure the object is one of the choices, else blank it out.
                final List<ObjectAdapter> choices = scalarModel
                            .getChoices(pendingArguments, getAuthenticationSession(), getDeploymentCategory());

                if(pendingArg.isValue()) {
                    // we have to do this if the ObjectAdapters are value type (eg a string)
                    //  because we can end up with a different ObjectAdapter for the same underlying value
                    //  (values have no intrinsic identity)

                    // it might not be necessary to have this special casing; we could probably use this code
                    // even for reference types
                    final Object pendingValue = pendingArg.getObject();
                    final List<Object> choiceValues = ObjectAdapter.Util.unwrap(choices);
                    if(!choiceValues.contains(pendingValue)) {
                        scalarModel.setObject(null);
                        scalarModel.setPending(null);
                        actionArgumentModel.setObject(null);
                    }
                } else {
                    if(!choices.contains(pendingArg)) {
                        scalarModel.setObject(null);
                        scalarModel.setPending(null);
                        actionArgumentModel.setObject(null);
                    }

                }

            }
        }

        // repaint the entire form if visibility has changed
        if (!visibilityBefore || !visibilityAfter) {
            return Repaint.ENTIRE_FORM;
        }

        // repaint the param if usability has changed
        if (!usabilityAfter || !usabilityBefore) {
            return Repaint.PARAM_ONLY;
        }

        // also repaint the param if its pending arg has changed.
        return scalarModel.getObject() != pendingArg
                ? Repaint.PARAM_ONLY
                : Repaint.NOTHING;
    }



    public static class InlinePromptConfig {
        private final boolean supported;
        private final Component componentToHideIfAny;

        public static InlinePromptConfig supported() {
            return new InlinePromptConfig(true, null);
        }

        public static InlinePromptConfig notSupported() {
            return new InlinePromptConfig(false, null);
        }

        public static InlinePromptConfig supportedAndHide(final Component componentToHideIfAny) {
            return new InlinePromptConfig(true, componentToHideIfAny);
        }

        private InlinePromptConfig(final boolean supported, final Component componentToHideIfAny) {
            this.supported = supported;
            this.componentToHideIfAny = componentToHideIfAny;
        }

        boolean isSupported() {
            return supported;
        }

        Component getComponentToHideIfAny() {
            return componentToHideIfAny;
        }
    }

    // ///////////////////////////////////////////////////////////////////

    protected final ScalarModel scalarModel;

    private Component scalarIfCompact;
    private MarkupContainer scalarIfRegular;

    private WebMarkupContainer scalarTypeContainer;

    /**
     * Populated
     * Used by most subclasses ({@link ScalarPanelAbstract2}, {@link ReferencePanel}, {@link ValueChoicesSelect2Panel}) but not all ({@link IsisBlobOrClobPanelAbstract}, {@link BooleanPanel})
     */
    private WebMarkupContainer scalarIfRegularInlinePromptForm;

    WebMarkupContainer inlinePromptLink;

    public ScalarPanelAbstract2(final String id, final ScalarModel scalarModel) {
        super(id, scalarModel);
        this.scalarModel = scalarModel;
    }


    // ///////////////////////////////////////////////////////////////////


    @Override
    protected void onInitialize() {
        super.onInitialize();

        buildGuiAndCallHooks();

        setOutputMarkupId(true);

    }

    private void buildGuiAndCallHooks() {

        try {
            buildGui();
        } catch (ConcurrencyException ex) {
            //
            // this has to be here because it's the first method called when editing a property
            // on a potentially stale model.
            //
            // there is similar code for invoking actions (ActionLink)
            //
            IsisContext.getSessionFactory().getCurrentSession().getAuthenticationSession().getMessageBroker().addMessage(ex.getMessage());
            final ObjectAdapter parentAdapter = getModel().getParentEntityModel().load();
            throw new RestartResponseException(new EntityPage(parentAdapter));
        }

        final ScalarModel scalarModel = getModel();

        final String disableReasonIfAny = scalarModel.whetherDisabled();
        final boolean mustBeEditable = scalarModel.mustBeEditable(getDeploymentCategory());
        if (disableReasonIfAny != null) {
            if(mustBeEditable) {
                onInitializeWhenViewMode();
            } else {
                onInitializeWhenDisabled(disableReasonIfAny);
            }
        } else {
            if (scalarModel.isViewMode()) {
                onInitializeWhenViewMode();
            } else {
                onInitializeWhenEnabled();
            }
        }

    }

    /**
     * Mandatory hook; simply determines the CSS that is added to the outermost 'scalarTypeContainer' div.
     */
    protected abstract String getScalarPanelType();

    /**
     * Mandatory hook for implementations to indicate whether it supports the {@link PromptStyle#INLINE inline} or
     * {@link PromptStyle#INLINE_AS_IF_EDIT prompt}s, and if so, how.
     *
     * <p>
     *     For those that do, both {@link #createInlinePromptForm()} and
     *     {@link #createInlinePromptLink()} must return non-null values (and their corresponding markup
     *     must define the corresponding elements).
     * </p>
     *
     * <p>
     *     Implementations that support inline prompts are: ({@link ScalarPanelAbstract2}, {@link ReferencePanel} and
     *     {@link ValueChoicesSelect2Panel}; those that don't are {@link IsisBlobOrClobPanelAbstract} and {@link BooleanPanel}.
     * </p>
     *
     */
    protected abstract InlinePromptConfig getInlinePromptConfig();




    /**
     * Builds GUI lazily prior to first render.
     *
     * <p>
     * This design allows the panel to be configured first.
     *
     * @see #onBeforeRender()
     */
    private void buildGui() {

        scalarTypeContainer = new WebMarkupContainer(ID_SCALAR_TYPE_CONTAINER);
        scalarTypeContainer.setOutputMarkupId(true);
        scalarTypeContainer.add(new CssClassAppender(Model.of(getScalarPanelType())));
        addOrReplace(scalarTypeContainer);

        final Rendering rendering = getRendering();


        if (rendering == Rendering.COMPACT) {
            this.scalarIfCompact = createComponentForCompact();
            this.scalarIfRegular = new WebMarkupContainer(ID_SCALAR_IF_REGULAR);
            this.scalarIfRegularInlinePromptForm = createInlinePromptForm();
            scalarTypeContainer.addOrReplace(scalarIfRegularInlinePromptForm);
        } else {
            this.scalarIfCompact = new WebMarkupContainer(ID_SCALAR_IF_COMPACT);
            this.scalarIfRegular = createComponentForRegular();
        }
        scalarTypeContainer.addOrReplace(scalarIfCompact, scalarIfRegular);

        if (rendering == Rendering.COMPACT) {
            // nothing else to do
        } else {
            scalarIfRegular.setOutputMarkupId(true);

            // find associated actions for this scalar property (only properties will have any.)
            final ScalarModel.AssociatedActions associatedActionsIfProperty =
                    scalarModel.associatedActionsIfProperty(getDeploymentCategory());
            final ObjectAction inlineActionIfAny =
                    associatedActionsIfProperty.getFirstAssociatedWithInlineAsIfEdit();
            final List<ObjectAction> remainingAssociated = associatedActionsIfProperty.getRemainingAssociated();

            // convert those actions into UI layer widgets
            final List<LinkAndLabel> linkAndLabels  = LinkAndLabelUtil.asActionLinks(this.scalarModel, remainingAssociated);

            final InlinePromptConfig inlinePromptConfig = getInlinePromptConfig();
            if(inlinePromptConfig.isSupported()) {

                this.scalarIfRegularInlinePromptForm = createInlinePromptForm();
                scalarTypeContainer.addOrReplace(scalarIfRegularInlinePromptForm);
                inlinePromptLink = createInlinePromptLink();
                scalarIfRegular.add(inlinePromptLink);

                // even if this particular scalarModel (property) is not configured for inline edits,
                // it's possible that one of the associated actions is.  Thus we set the prompt context
                scalarModel.setInlinePromptContext(
                        new InlinePromptContext(
                                getComponentForRegular(),
                                scalarIfRegularInlinePromptForm, scalarTypeContainer));

                // start off assuming that neither the property nor any of the associated actions
                // are using inline prompts
                Component componentToHideIfAny = inlinePromptLink;

                if (this.scalarModel.getPromptStyle().isInline() && scalarModel.canEnterEditMode()) {
                    // we configure the prompt link if _this_ property is configured for inline edits...
                    configureInlinePromptLinkCallback(inlinePromptLink);
                    componentToHideIfAny = inlinePromptConfig.getComponentToHideIfAny();

                } else {

                    // not editable property, but maybe one of the actions is.
                    if(inlineActionIfAny != null) {

                        final LinkAndLabel linkAndLabelAsIfEdit = LinkAndLabelUtil
                                .asActionLink(this.scalarModel, inlineActionIfAny);
                        final ActionLink actionLinkInlineAsIfEdit = (ActionLink) linkAndLabelAsIfEdit.getLink();

                        if(actionLinkInlineAsIfEdit.isVisible() && actionLinkInlineAsIfEdit.isEnabled()) {
                            configureInlinePromptLinkCallback(inlinePromptLink, actionLinkInlineAsIfEdit);
                            componentToHideIfAny = inlinePromptConfig.getComponentToHideIfAny();
                        }
                    }
                }


                if(componentToHideIfAny != null) {
                    componentToHideIfAny.setVisibilityAllowed(false);
                }
            }

            // prevent from tabbing into non-editable widgets.
            if(scalarModel.getKind() == ScalarModel.Kind.PROPERTY &&
                    scalarModel.getMode() == EntityModel.Mode.VIEW     &&
                    (scalarModel.getPromptStyle().isDialog() || !scalarModel.canEnterEditMode())) {
                getScalarValueComponent().add(new AttributeAppender("tabindex", "-1"));
            }

            addPositioningCssTo(scalarIfRegular, linkAndLabels);
            addActionLinksBelowAndRight(scalarIfRegular, linkAndLabels);

            addEditPropertyTo(scalarIfRegular);
            addFeedbackOnlyTo(scalarIfRegular, getScalarValueComponent());

        }

        getRendering().buildGui(this);
        addCssFromMetaModel();

        notifyOnChange(this);
        addFormComponentBehaviourToUpdateSubscribers();

    }

    /**
     * The widget starts off in read-only, but should be possible to activate into edit mode.
     *
     * <p>
     *     TODO: perhaps rename to 'notEditable'
     * </p>
     */
    protected void onInitializeWhenViewMode() {
    }

    /**
     * The widget starts off read-only, and CANNOT be activated into edit mode.
     *
     * <p>
     *     TODO: perhaps rename to 'readOnly'
     * </p>
     */
    protected void onInitializeWhenDisabled(final String disableReason) {
    }

    /**
     * The widget starts off immediately editable.
     *
     * <p>
     *     TODO: perhaps rename to 'editable'.
     * </p>
     */
    protected void onInitializeWhenEnabled() {
    }

    /**
     * The widget is no longer editable.
     *
     * <p>
     *     TODO: perhaps rename to 'onNotEditable', because the semantics here aren't the same as 'onInitializeWhenDisabled'
     *       (the latter is never editable).
     * </p>
     */
    protected void onDisabled(final String disableReason, final AjaxRequestTarget target) {
    }

    /**
     * The widget should be made editable.
     *
     * <p>
     *     TODO: perhaps rename to 'onEditable'?
     * </p>
     */
    protected void onEnabled(final AjaxRequestTarget target) {
    }



    private void addCssFromMetaModel() {
        final String cssForMetaModel = getModel().getCssClass();
        if (!Strings.isNullOrEmpty(cssForMetaModel)) {
            CssClassAppender.appendCssClassTo(this, CssClassAppender.asCssStyle(cssForMetaModel));
        }

        ScalarModel model = getModel();
        final CssClassFacet facet = model.getFacet(CssClassFacet.class);
        if(facet != null) {

            final ObjectAdapter parentAdapter =
                    model.getParentEntityModel().load(AdapterManager.ConcurrencyChecking.NO_CHECK);

            final String cssClass = facet.cssClass(parentAdapter);
            CssClassAppender.appendCssClassTo(this, cssClass);
        }
    }


    // //////////////////////////////////////

    /**
     * Each component is now responsible for determining if it should be visible or not.
     *
     * <p>
     * Unlike the constructor and <tt>onInitialize</tt>, which are called only once, the <tt>onConfigure</tt> callback
     * is called multiple times, just prior to <tt>onBeforeRendering</tt>.  It is therefore the correct place for
     * components to set up their visibility/enablement.
     * </p>
     *
     */
    @Override
    protected void onConfigure() {

        final ScalarModel scalarModel = getModel();

        final boolean hidden = scalarModel.whetherHidden();
        setVisibilityAllowed(!hidden);

        super.onConfigure();
    }


    // //////////////////////////////////////


    static class ScalarUpdatingBehavior extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        private final ScalarPanelAbstract2 scalarPanel;

        private ScalarUpdatingBehavior(final ScalarPanelAbstract2 scalarPanel) {
            super("change");
            this.scalarPanel = scalarPanel;
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            for (ScalarModelSubscriber2 subscriber : scalarPanel.subscribers) {
                subscriber.onUpdate(target, scalarPanel);
            }
        }

        @Override
        protected void onError(AjaxRequestTarget target, RuntimeException e) {
            super.onError(target, e);
            for (ScalarModelSubscriber2 subscriber : scalarPanel.subscribers) {
                subscriber.onError(target, scalarPanel);
            }
        }
    }

    private final List<ScalarModelSubscriber2> subscribers = Lists.newArrayList();

    public void notifyOnChange(final ScalarModelSubscriber2 subscriber) {
        subscribers.add(subscriber);
    }

    private void addFormComponentBehaviourToUpdateSubscribers() {
        Component scalarValueComponent = getScalarValueComponent();
        if(scalarValueComponent == null) {
            return;
        }
        for (Behavior b : scalarValueComponent.getBehaviors(ScalarUpdatingBehavior.class)) {
            scalarValueComponent.remove(b);
        }
        scalarValueComponent.add(new ScalarUpdatingBehavior(this));
    }

    // //////////////////////////////////////

    @Override
    public void onUpdate(
            final AjaxRequestTarget target, final ScalarPanelAbstract2 scalarPanel) {

        if(getModel().getKind() == ScalarModel.Kind.PARAMETER) {
            target.appendJavaScript(
                    String.format("Wicket.Event.publish(Isis.Topic.FOCUS_FIRST_PARAMETER, '%s')", getMarkupId()));
        }
    }


    @Override
    public void onError(
            final AjaxRequestTarget target, final ScalarPanelAbstract2 scalarPanel) {

    }


    // ///////////////////////////////////////////////////////////////////

    
    public enum Rendering {
        /**
         * Does not show labels, eg for use in tables
         */
        COMPACT {
            @Override
            public String getLabelCaption(final LabeledWebMarkupContainer labeledContainer) {
                return "";
            }

            @Override
            public void buildGui(final ScalarPanelAbstract2 panel) {
                panel.getComponentForRegular().setVisible(false);
            }

        },
        /**
         * Does show labels, eg for use in forms.
         */
        REGULAR {
            @Override
            public String getLabelCaption(final LabeledWebMarkupContainer labeledContainer) {
                return labeledContainer.getLabel().getObject();
            }

            @Override
            public void buildGui(final ScalarPanelAbstract2 panel) {
                panel.scalarIfCompact.setVisible(false);
            }

        };

        public abstract String getLabelCaption(LabeledWebMarkupContainer labeledContainer);

        public abstract void buildGui(ScalarPanelAbstract2 panel);

        private static Rendering renderingFor(EntityModel.RenderingHint renderingHint) {
            return renderingHint.isRegular()? Rendering.REGULAR :Rendering.COMPACT;
        }
    }

    protected Rendering getRendering() {
        return Rendering.renderingFor(scalarModel.getRenderingHint());
    }

    // ///////////////////////////////////////////////////////////////////

    protected Component getComponentForRegular() {
        return scalarIfRegular;
    }

    /**
     * Mandatory hook method to build the component to render the model when in
     * {@link Rendering#REGULAR regular} format.
     *
     * <p>
     *     Is added to {@link #scalarTypeContainer}.
     * </p>
     */
    protected abstract MarkupContainer createComponentForRegular();

    /**
     * Mandatory hook method to build the component to render the model when in
     * {@link Rendering#COMPACT compact} format.
     *
     * <p>
     *     Is added to {@link #scalarTypeContainer}.
     * </p>
     */
    protected abstract Component createComponentForCompact();

    protected Label createScalarName(final String id, final String labelCaption) {
        final Label scalarName = new Label(id, labelCaption);
        final ScalarModel model = getModel();
        if(model.isRequired() && model.isEnabled()) {
            final String label = scalarName.getDefaultModelObjectAsString();
            if(!Strings.isNullOrEmpty(label)) {
                scalarName.add(new CssClassAppender("mandatory"));
            }
        }
        NamedFacet namedFacet = model.getFacet(NamedFacet.class);
        if (namedFacet != null) {
            scalarName.setEscapeModelStrings(namedFacet.escaped());
        }
        return scalarName;
    }

    /**
     * Returns a container holding an empty form.  This can be switched out using {@link #switchFormForInlinePrompt(AjaxRequestTarget)}.
     */
    private WebMarkupContainer createInlinePromptForm() {

        // (placeholder initially, create dynamically when needed - otherwise infinite loop because form references regular)

        WebMarkupContainer scalarIfRegularInlinePromptForm =
                new WebMarkupContainer( ID_SCALAR_IF_REGULAR_INLINE_PROMPT_FORM);
        scalarIfRegularInlinePromptForm.setOutputMarkupId(true);
        scalarIfRegularInlinePromptForm.setVisible(false);

        return scalarIfRegularInlinePromptForm;
    }

    private WebMarkupContainer createInlinePromptLink() {
        final IModel<String> inlinePromptModel = obtainInlinePromptModel();
        if(inlinePromptModel == null) {
            throw new IllegalStateException(this.getClass().getName() + ": obtainInlinePromptModel() returning null is not compatible with supportsInlinePrompt() returning true ");
        }

        final WebMarkupContainer inlinePromptLink = new WebMarkupContainer(ID_SCALAR_VALUE_INLINE_PROMPT_LINK);
        inlinePromptLink.setOutputMarkupId(true);
        inlinePromptLink.setOutputMarkupPlaceholderTag(true);

        configureInlinePromptLink(inlinePromptLink);

        final Component editInlineLinkLabel = createInlinePromptComponent(ID_SCALAR_VALUE_INLINE_PROMPT_LABEL,
                inlinePromptModel
        );
        inlinePromptLink.add(editInlineLinkLabel);

        return inlinePromptLink;
    }

    protected void configureInlinePromptLink(final WebMarkupContainer inlinePromptLink) {
        final String append = obtainInlinePromptLinkCssIfAny();
        if(append != null) {
            inlinePromptLink.add(new CssClassAppender(append));
        }
    }

    protected String obtainInlinePromptLinkCssIfAny() {
        return "form-control input-sm";
    }

    protected Component createInlinePromptComponent(
            final String id, final IModel<String> inlinePromptModel) {
        return new Label(id, inlinePromptModel) {
            @Override protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("tabindex","-1");
            }
        };
    }

    // ///////////////////////////////////////////////////////////////////

    /**
     * Components returning true for {@link #getInlinePromptConfig()} are required to override and return a non-null value.
     */
    protected IModel<String> obtainInlinePromptModel() {
        return null;
    }


    private void configureInlinePromptLinkCallback(final WebMarkupContainer inlinePromptLink) {

        inlinePromptLink.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(final AjaxRequestTarget target) {

                scalarModel.toEditMode();

                switchFormForInlinePrompt(target);

                getComponentForRegular().setVisible(false);
                scalarIfRegularInlinePromptForm.setVisible(true);

                target.add(scalarTypeContainer);
            }

            @Override
            public boolean isEnabled(final Component component) {
                return true;
            }
        });
    }

    private void configureInlinePromptLinkCallback(
            final WebMarkupContainer inlinePromptLink,
            final ActionLink actionLink) {

        inlinePromptLink.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                actionLink.onClick(target);
            }

            @Override
            public boolean isEnabled(final Component component) {
                return true;
            }
        });
    }

    private void switchFormForInlinePrompt(final AjaxRequestTarget target) {
        scalarIfRegularInlinePromptForm = (PropertyEditFormPanel) getComponentFactoryRegistry().addOrReplaceComponent(
                scalarTypeContainer, ID_SCALAR_IF_REGULAR_INLINE_PROMPT_FORM, ComponentType.PROPERTY_EDIT_FORM, scalarModel);

        onSwitchFormForInlinePrompt(scalarIfRegularInlinePromptForm, target);
    }

    /**
     * Optional hook.
     */
    protected void onSwitchFormForInlinePrompt(
            final WebMarkupContainer inlinePromptForm,
            final AjaxRequestTarget target) {

    }


    // ///////////////////////////////////////////////////////////////////

    protected void addEditPropertyTo(
            final MarkupContainer scalarIfRegularFormGroup) {

        final PromptStyle promptStyle = scalarModel.getPromptStyle();
        if(  scalarModel.canEnterEditMode() &&
            (promptStyle.isDialog() ||
            !getInlinePromptConfig().isSupported())) {

            final WebMarkupContainer editProperty = new WebMarkupContainer(ID_EDIT_PROPERTY);
            editProperty.setOutputMarkupId(true);
            scalarIfRegularFormGroup.addOrReplace(editProperty);

            editProperty.add(new AjaxEventBehavior("click") {
                protected void onEvent(AjaxRequestTarget target) {

                    final ObjectSpecification specification = scalarModel.getTypeOfSpecification();
                    final MetaModelService2 metaModelService2 = getIsisSessionFactory().getServicesInjector()
                            .lookupService(MetaModelService2.class);
                    final MetaModelService2.Sort sort = metaModelService2.sortOf(specification.getCorrespondingClass());

                    final ActionPrompt prompt = ActionPromptProvider.Util
                            .getFrom(ScalarPanelAbstract2.this).getActionPrompt(promptStyle, sort);

                    PropertyEditPromptHeaderPanel titlePanel = new PropertyEditPromptHeaderPanel(prompt.getTitleId(),
                            ScalarPanelAbstract2.this.scalarModel);

                    final PropertyEditPanel propertyEditPanel =
                            (PropertyEditPanel) getComponentFactoryRegistry().createComponent(
                                    ComponentType.PROPERTY_EDIT_PROMPT, prompt.getContentId(),
                                    ScalarPanelAbstract2.this.scalarModel);

                    propertyEditPanel.setShowHeader(false);

                    prompt.setTitle(titlePanel, target);
                    prompt.setPanel(propertyEditPanel, target);
                    prompt.showPrompt(target);

                }
            });
        } else {
            Components.permanentlyHide(scalarIfRegularFormGroup, ID_EDIT_PROPERTY);
        }

    }

    /**
     * Mandatory hook, used to determine which component to attach feedback to.
     * @return
     */
    protected abstract Component getScalarValueComponent();


    private void addFeedbackOnlyTo(final MarkupContainer markupContainer, final Component component) {
        markupContainer.addOrReplace(new NotificationPanel(ID_FEEDBACK, component, new ComponentFeedbackMessageFilter(component)));
    }

    private void addActionLinksBelowAndRight(
            final MarkupContainer labelIfRegular,
            final List<LinkAndLabel> linkAndLabels) {
        final List<LinkAndLabel> linksBelow = LinkAndLabel.positioned(linkAndLabels, ActionLayout.Position.BELOW);
        AdditionalLinksPanel.addAdditionalLinks(labelIfRegular, ID_ASSOCIATED_ACTION_LINKS_BELOW, linksBelow, AdditionalLinksPanel.Style.INLINE_LIST);

        final List<LinkAndLabel> linksRight = LinkAndLabel.positioned(linkAndLabels, ActionLayout.Position.RIGHT);
        AdditionalLinksPanel.addAdditionalLinks(labelIfRegular, ID_ASSOCIATED_ACTION_LINKS_RIGHT, linksRight, AdditionalLinksPanel.Style.DROPDOWN);
    }

    /**
     * Applies the {@literal @}{@link LabelAtFacet} and also CSS based on
     * whether any of the associated actions have {@literal @}{@link ActionLayout layout} positioned to
     * the {@link ActionLayout.Position#RIGHT right}.
     *
     * @param markupContainer The form group element
     * @param actionLinks
     */
    private void addPositioningCssTo(
            final MarkupContainer markupContainer,
            final List<LinkAndLabel> actionLinks) {
        CssClassAppender.appendCssClassTo(markupContainer, determinePropParamLayoutCss(getModel()));
        CssClassAppender.appendCssClassTo(markupContainer, determineActionLayoutPositioningCss(actionLinks));
    }

    private static String determinePropParamLayoutCss(ScalarModel model) {
        final LabelAtFacet facet = model.getFacet(LabelAtFacet.class);
        if (facet != null) {
            switch (facet.label()) {
            case LEFT:
                return "label-left";
            case RIGHT:
                return "label-right";
            case NONE:
                return "label-none";
            case TOP:
                return "label-top";
            }
        }
        return "label-left";
    }

    private static String determineActionLayoutPositioningCss(List<LinkAndLabel> entityActionLinks) {
        boolean actionsPositionedOnRight = hasActionsPositionedOn(entityActionLinks, ActionLayout.Position.RIGHT);
        return actionsPositionedOnRight ? "actions-right" : null;
    }

    private static boolean hasActionsPositionedOn(final List<LinkAndLabel> entityActionLinks, final ActionLayout.Position position) {
        for (LinkAndLabel entityActionLink : entityActionLinks) {
            if(entityActionLink.getPosition() == position) {
                return true;
            }
        }
        return false;
    }

    // ///////////////////////////////////////////////////////////////////

    /**
     * Repaints this panel of just some of its children
     *
     * @param target The Ajax request handler
     */
    public void repaint(AjaxRequestTarget target) {
        target.add(this);
    }


}
