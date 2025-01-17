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

package org.apache.isis.viewer.wicket.ui.components.scalars.primitive;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import org.apache.isis.applib.annotation.LabelPosition;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.objectvalue.labelat.LabelAtFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelAbstract2;
import org.apache.isis.viewer.wicket.ui.components.widgets.bootstrap.FormGroup;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkboxx.CheckBoxX;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkboxx.CheckBoxXConfig;
import de.agilecoders.wicket.jquery.Key;

/**
 * Panel for rendering scalars of type {@link Boolean} or <tt>boolean</tt>.
 */
public class BooleanPanel extends ScalarPanelAbstract2 {

    private static final long serialVersionUID = 1L;

    private CheckBoxX checkBox;

    public BooleanPanel(final String id, final ScalarModel scalarModel) {
        super(id, scalarModel);
    }

    @Override
    protected MarkupContainer createComponentForRegular() {
        final String name = getModel().getName();

        checkBox = createCheckBox(ID_SCALAR_VALUE, CheckBoxXConfig.Sizes.lg);

        checkBox.setLabel(Model.of(name));

        final FormGroup scalarIfRegularFormGroup = new FormGroup(ID_SCALAR_IF_REGULAR, checkBox);
        scalarIfRegularFormGroup.add(checkBox);
        if(getModel().isRequired() && getModel().isEnabled()) {
            scalarIfRegularFormGroup.add(new CssClassAppender("mandatory"));
        }

        final String labelCaption = getRendering().getLabelCaption(checkBox);
        final Label scalarName = createScalarName(ID_SCALAR_NAME, labelCaption);

        scalarIfRegularFormGroup.add(scalarName);

        final String describedAs = getModel().getDescribedAs();
        if(describedAs != null) {
            scalarIfRegularFormGroup.add(new AttributeModifier("title", Model.of(describedAs)));
        }


        return scalarIfRegularFormGroup;
    }

    protected Component getScalarValueComponent() {
        return checkBox;
    }

    /**
     * Mandatory hook method to build the component to render the model when in
     * {@link Rendering#COMPACT compact} format.
     */
    @Override
    protected Component createComponentForCompact() {
        return createCheckBox(ID_SCALAR_IF_COMPACT, CheckBoxXConfig.Sizes.sm);
    }


    @Override
    protected InlinePromptConfig getInlinePromptConfig() {
        return InlinePromptConfig.supportedAndHide(
                // TODO: not sure why this is needed when the other subtypes have no similar guard...
                scalarModel.mustBeEditable(getDeploymentCategory())
                        ? this.checkBox
                        : null
        );
    }

    @Override
    protected IModel<String> obtainInlinePromptModel() {
        return new Model<String>() {

            private static final long serialVersionUID = 1L;

            @Override public String getObject() {
                final ScalarModel model = getModel();
                final ObjectAdapter adapter = model.getObject();
                final Boolean bool = adapter != null ? (Boolean) adapter.getObject() : null;
                return bool == null? "(not set)" : bool ? "Yes" : "No";
            }
        };
    }

    private CheckBoxX createCheckBox(final String id, final CheckBoxXConfig.Sizes size) {

        final CheckBoxXConfig config = configFor(getModel().isRequired(), size);

        final CheckBoxX checkBox = new CheckBoxX(id, new Model<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                final ScalarModel model = getModel();
                final ObjectAdapter adapter = model.getObject();
                return adapter != null? (Boolean) adapter.getObject(): null;
            }

            @Override
            public void setObject(final Boolean object) {
                final ObjectAdapter adapter = getPersistenceSession().adapterFor(object);
                getModel().setObject(adapter);
            }
        }) {
            @Override
            public CheckBoxXConfig getConfig() {
                return config;
            }

            @Override protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                //
                // this is a horrid hack to allow the space bar to work as a way of toggling the checkbox.
                // this hack works for 1.5.4 of the JS plugin (https://github.com/kartik-v/bootstrap-checkbox-x)
                //
                // the problem is that the "change" event is not fired for a keystroke; instead the callback in the
                // JS code (https://github.com/kartik-v/bootstrap-checkbox-x/blob/v1.5.4/js/checkbox-x.js#L70)
                // calls self.change().  This in turn calls validateCheckbox().  In that method it is possible to
                // cause the "change" event to fire, but only if the input element is NOT type="checkbox".
                // (https://github.com/kartik-v/bootstrap-checkbox-x/blob/v1.5.4/js/checkbox-x.js#L132)
                //
                // It's not possible to simply change the associated markup to input type='xx' because it falls foul
                // of a check in super.onComponentTag(tag).  So instead we let that through then hack the tag
                // afterwards:
                //
                tag.put("type", "xx");
            }
        };
        checkBox.setOutputMarkupId(true);
        checkBox.setEnabled(false); // will be enabled before rendering if
                                    // required

        // must prime the underlying model if this is a primitive boolean
        final ObjectSpecification objectSpecification = getModel().getTypeOfSpecification();
        if(objectSpecification.getFullIdentifier().equals("boolean")) {
            if(getModel().getObject() == null) {
                getModel().setObject(getPersistenceSession().adapterFor(false));
            }
        }

        return checkBox;
    }

    private static CheckBoxXConfig configFor(final boolean required, final CheckBoxXConfig.Sizes size) {
        final CheckBoxXConfig config = new CheckBoxXConfig() {
            {
                // so can tab to the checkbox
                // not part of the API, so have to use this object initializer
                put(new Key<String>("tabindex"), "0");
            }
        };
        return config
                .withSize(size)
                .withEnclosedLabel(false)
                .withIconChecked("<i class='fa fa-fw fa-check'></i>")
                .withIconNull("<i class='fa fa-fw fa-square'></i>")
                .withThreeState(!required);
    }

    @Override
    protected void onInitializeWhenEnabled() {
        super.onInitializeWhenEnabled();
        checkBox.setEnabled(true);
    }

    @Override
    protected void onInitializeWhenViewMode() {
        super.onInitializeWhenViewMode();
        if(getRendering() == Rendering.COMPACT) {
            return;
        }
        checkBox.setEnabled(false);
    }

    @Override
    protected void onInitializeWhenDisabled(final String disableReason) {
        super.onInitializeWhenDisabled(disableReason);
        if(getRendering() == Rendering.COMPACT) {
            return;
        }
        checkBox.setEnabled(false);
        final AttributeModifier title = new AttributeModifier("title",
                                                Model.of(disableReason != null ? disableReason : ""));
        checkBox.add(title);
    }

    @Override
    protected void onDisabled(final String disableReason, final AjaxRequestTarget target) {
        super.onDisabled(disableReason, target);
        if(getRendering() == Rendering.COMPACT) {
            return;
        }

        checkBox.setEnabled(false);
        final AttributeModifier title = new AttributeModifier("title",
                                                Model.of(disableReason != null ? disableReason : ""));
        checkBox.add(title);
        target.add(checkBox);
    }

    @Override
    protected void onEnabled(final AjaxRequestTarget target) {
        super.onEnabled(target);
        if(getRendering() == Rendering.COMPACT) {
            return;
        }
        checkBox.setEnabled(true);
    }

    @Override
    public String getVariation() {
        String variation;
        final LabelAtFacet facet = getModel().getFacet(LabelAtFacet.class);
        if (facet != null && LabelPosition.RIGHT == facet.label()) {
            variation = "labelRightPosition";
        } else {
            variation = super.getVariation();
        }
        return variation;
    }

    @Override
    protected String getScalarPanelType() {
        return "booleanPanel";
    }


}
