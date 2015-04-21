package edu.ualberta.med.biobank.gui.common.widgets.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.widgets.FormToolkit;

import edu.ualberta.med.biobank.gui.common.forms.FieldInfo;
import edu.ualberta.med.biobank.gui.common.validators.AbstractValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseWidget;
import edu.ualberta.med.biobank.gui.common.widgets.DateTimeWidget;
import edu.ualberta.med.biobank.util.NullUtil;

public class BgcWidgetCreator {

    protected DataBindingContext dbc;

    protected Map<String, Control> controls;

    protected Map<String, Binding> bindings;

    private FormToolkit toolkit;

    private KeyListener keyListener;

    private ModifyListener modifyListener;

    private SelectionListener selectionListener;

    public static final Color READ_ONLY_TEXT_BGR = Display.getCurrent()
        .getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

    public BgcWidgetCreator(Map<String, Control> controls) {
        this.controls = controls;
    }

    public void initDataBinding() {
        dbc = new DataBindingContext();
        bindings = new HashMap<String, Binding>();
    }

    public void setToolkit(FormToolkit toolkit) {
        this.toolkit = toolkit;
    }

    public FormToolkit getToolkit() {
        return toolkit;
    }

    public void createBoundWidgetsFromMap(Map<String, FieldInfo> fieldsMap,
        Object bean, Composite client) {
        for (Entry<String, FieldInfo> entry : fieldsMap.entrySet()) {
            String label = entry.getKey();
            FieldInfo fi = entry.getValue();
            Control control = createBoundWidgetWithLabel(client,
                fi.widgetClass, fi.widgetOptions, fi.label, fi.widgetValues,
                BeansObservables.observeValue(bean, label), fi.validatorClass,
                fi.errMsg);
            if (controls != null) {
                controls.put(label, control);
            }
        }
    }

    public Control createBoundWidgetWithLabel(Composite composite,
        Class<? extends Widget> widgetClass, int widgetOptions,
        String fieldLabel, String[] widgetValues,
        IObservableValue modelObservableValue,
        Class<? extends AbstractValidator> validatorClass,
        String validatorErrMsg) {
        AbstractValidator validator = null;
        if (validatorClass != null) {
            validator = createValidator(validatorClass, validatorErrMsg);
        }
        return createBoundWidgetWithLabel(composite, widgetClass,
            widgetOptions, fieldLabel, widgetValues, modelObservableValue,
            validator);
    }

    public Control createBoundWidgetWithLabel(Composite composite,
        Class<? extends Widget> widgetClass, int widgetOptions,
        String fieldLabel, String[] widgetValues,
        IObservableValue modelObservableValue, AbstractValidator validator) {
        return createBoundWidgetWithLabel(composite, widgetClass,
            widgetOptions, fieldLabel, widgetValues, modelObservableValue,
            validator, null);
    }

    public Control createBoundWidgetWithLabel(Composite composite,
        Class<? extends Widget> widgetClass, int widgetOptions,
        String fieldLabel, String[] widgetValues,
        IObservableValue modelObservableValue, AbstractValidator validator,
        String bindingKey) {
        Label label;

        label = createLabel(composite, fieldLabel);
        int gridDataStyle = GridData.VERTICAL_ALIGN_CENTER;
        if (widgetClass.equals(BgcBaseText.class)
            && ((widgetOptions & SWT.MULTI) != 0)) {
            gridDataStyle = GridData.VERTICAL_ALIGN_BEGINNING;
        }
        label.setLayoutData(new GridData(gridDataStyle));
        return createBoundWidget(composite, widgetClass, widgetOptions, label,
            widgetValues, modelObservableValue, validator, bindingKey);

    }

    public Control createBoundWidget(Composite composite,
        Class<? extends Widget> widgetClass, int widgetOptions,
        String[] widgetValues, IObservableValue modelObservableValue,
        IValidator validator) {
        return createBoundWidget(composite, widgetClass, widgetOptions,
            widgetValues, modelObservableValue, validator, null);
    }

    @SuppressWarnings("nls")
    public Control createBoundWidget(Composite composite, Class<? extends Widget> widgetClass,
        int widgetOptions, String[] widgetValues, IObservableValue modelObservableValue,
        IValidator validator, String bindingKey) {
        Assert.isNotNull(dbc);
        UpdateValueStrategy uvs = null;
        if (validator != null) {
            uvs = new UpdateValueStrategy();
            uvs.setAfterGetValidator(validator);
        }
        if (widgetClass == BgcBaseText.class) {
            return createText(composite, widgetOptions, modelObservableValue, uvs, bindingKey);
        } else if (widgetClass == Combo.class) {
            return createCombo(composite, widgetOptions, widgetValues, modelObservableValue, uvs,
                bindingKey);
        } else if (widgetClass == Button.class) {
            return createButton(composite, modelObservableValue, uvs, bindingKey);
        } else {
            Assert.isTrue(false, "invalid widget class " + widgetClass.getName());
        }
        return null;
    }

    private Control createButton(Composite composite,
        IObservableValue modelObservableValue, UpdateValueStrategy uvs,
        String bindingKey) {
        Button button = new Button(composite, SWT.CHECK);
        if (toolkit != null) {
            toolkit.adapt(button, true, true);
        }
        Binding binding = dbc.bindValue(
            SWTObservables.observeSelection(button), modelObservableValue, uvs,
            null);
        if (bindingKey != null) {
            bindings.put(bindingKey, binding);
        }
        if (selectionListener != null) {
            button.addSelectionListener(selectionListener);
        }
        return button;
    }

    public void addBindingToMap(String bindingKey, Binding binding) {
        bindings.put(bindingKey, binding);
    }

    @SuppressWarnings("nls")
    private Combo createCombo(Composite composite, int options,
        String[] widgetValues, final IObservableValue modelObservableValue,
        UpdateValueStrategy uvs, String bindingKey) {
        final Combo combo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER
            | options);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        Assert.isNotNull(widgetValues, "combo values not assigned");
        combo.setItems(widgetValues);
        if (toolkit != null) {
            toolkit.adapt(combo, true, true);
        }
        Binding binding = dbc.bindValue(SWTObservables.observeSelection(combo),
            modelObservableValue, uvs, null);
        if (bindingKey != null) {
            bindings.put(bindingKey, binding);
        }
        if (selectionListener != null) {
            combo.addSelectionListener(selectionListener);
        }
        if (modifyListener != null) {
            combo.addModifyListener(modifyListener);
        }
        final IValueChangeListener changeListener = new IValueChangeListener() {
            @Override
            public void handleValueChange(ValueChangeEvent event) {
                if ((event.getObservableValue().getValue() == null)
                    || event.getObservableValue().getValue().toString().isEmpty()) {
                    combo.deselectAll();
                }
            }
        };
        modelObservableValue.addValueChangeListener(changeListener);
        combo.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                modelObservableValue.removeValueChangeListener(changeListener);
            }
        });
        combo.addListener(SWT.MouseWheel, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });
        return combo;
    }

    public BgcBaseText createText(Composite composite, int widgetOptions,
        IObservableValue modelObservableValue, UpdateValueStrategy uvs) {
        return createText(composite, widgetOptions, modelObservableValue, uvs,
            null);
    }

    public BgcBaseText createText(Composite composite, int widgetOptions,
        final IObservableValue modelObservableValue, UpdateValueStrategy uvs,
        String bindingKey) {
        if (widgetOptions == SWT.NONE) {
            widgetOptions = SWT.SINGLE;
        }

        if ((widgetOptions & SWT.MULTI) != 0) {
            widgetOptions = widgetOptions | SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.WRAP;
        }

        BgcBaseText text = null;
        if (toolkit == null) {
            text = new BgcBaseText(composite, widgetOptions);
        } else {
            text = new BgcBaseText(composite, widgetOptions, toolkit);
        }
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        if ((widgetOptions & SWT.MULTI) != 0) {
            gd.heightHint = 50;
            text.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.keyCode == 9) { // TAB
                        ((Control) e.widget).traverse(SWT.TRAVERSE_TAB_NEXT);
                        // cancel default tab behaviour of the text
                        e.doit = false;
                    }
                }
            });
        }
        // Integer and Double fields should not accept alpha characters. Really
        // should block all symbols but . and +- too. FIXME for a later date
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((modelObservableValue != null)
                    && (modelObservableValue.getValueType().equals(Integer.class)
                    || modelObservableValue.getValueType().equals(Double.class))
                    && Character.isLetter(e.character))
                    e.doit = false;
            }
        });
        text.setLayoutData(gd);
        if (keyListener != null) {
            text.addKeyListener(keyListener);
        }

        if (modelObservableValue != null) {
            // If the UpdateValueStrategy has an IValidator, then values that do
            // not validate will not be copied to the model object. This is a
            // problem in the following scenario: (1) model property has value
            // "asdf", which is valid (2) target property's value is immediately
            // changed to an invalid one (i.e. not through any valid
            // intermediate value) (3) target property's value is changed back
            // to "asdf". The problem is that since (1), the model property's
            // value has been "asdf" since its value was not switched to the
            // invalid value (because it did not pass validation). So, when the
            // model property's value is changed back to "asdf", the old value
            // and new value are both "asdf", so the target is not notified of
            // the "change" because there was none. Note that this is a problem
            // only when the SWT Object can be set to an invalid state (so
            // should be fine for Combos and Buttons).
            //
            // Ultimately, we want our model and target values to always be
            // synchronized. The validators are used by the GUI to indicate
            // problem(s). So, the text-field is bound to the model property
            // without validation and the text-field is bound to some observable
            // string whose sole purpose is to validate the text-field's value.
            dbc.bindValue(
                SWTObservables.observeText(text.getTextWidget(), SWT.Modify),
                modelObservableValue, null, null);
            Binding binding = dbc
                .bindValue(SWTObservables.observeText(text.getTextWidget(),
                    SWT.Modify),
                    new WritableValue(modelObservableValue.getValue(),
                        String.class), uvs, null);
            if (bindingKey != null) {
                bindings.put(bindingKey, binding);
            }
        }
        return text;
    }

    public Control createBoundWidget(Composite composite,
        Class<? extends Widget> widgetClass, int widgetOptions, Label label,
        String[] widgetValues, IObservableValue modelObservableValue,
        AbstractValidator validator) {
        return createBoundWidget(composite, widgetClass, widgetOptions, label,
            widgetValues, modelObservableValue, validator, null);
    }

    public Control createBoundWidget(Composite composite,
        Class<? extends Widget> widgetClass, int widgetOptions, Label label,
        String[] widgetValues, IObservableValue modelObservableValue,
        AbstractValidator validator, String bindingKey) {

        if (validator != null) {
            validator.setControlDecoration(BgcBaseWidget.createDecorator(label,
                validator.getErrorMessage()));
        }

        return createBoundWidget(composite, widgetClass, widgetOptions,
            widgetValues, modelObservableValue, validator, bindingKey);
    }

    public <T> ComboViewer createComboViewerWithoutLabel(Composite parent,
        Collection<T> input, T selection, IBaseLabelProvider labelProvider) {
        return createComboViewer(parent, null, input, selection, null, null,
            labelProvider);
    }

    /**
     * Create combo viewer with no validator on selection and with the default comparator.
     */
    public <T> ComboViewer createComboViewer(Composite parent,
        String fieldLabel, Collection<T> input, T selection,
        IBaseLabelProvider labelProvider) {
        return createComboViewer(parent, fieldLabel, input, selection, null,
            null, labelProvider);
    }

    /**
     * Create a combo viewer with a validator when selection is null using errorMessage and using
     * the default comparator.
     * 
     * @param parent the parent composite.
     * @param fieldLabel the label to be displayed to the left of the combo box.
     * @param input the items to select from.
     * @param selection the default selection.
     * @param errorMessage the error message to display if nothing is selected.
     * @param csu when not null, the callback interface to invoke when an item is selected.
     * @param labelProvider the label provider for the combo input.
     * 
     * @return The combo viewer.
     */
    public <T> ComboViewer createComboViewer(
        Composite parent,
        String fieldLabel,
        Collection<? extends T> input,
        T selection,
        String errorMessage,
        final ComboSelectionUpdate csu,
        IBaseLabelProvider labelProvider) {
        return createComboViewer(parent, fieldLabel, input, selection, errorMessage, true, csu,
            labelProvider);
    }

    /**
     * Create a combo viewer with a validator when selection is null using errorMessage and using
     * the default comparator if useDefaultComparator is set to true.
     * 
     * @param parent the parent composite.
     * @param fieldLabel the label to be displayed to the left of the combo box.
     * @param input the items to select from.
     * @param selection the default selection.
     * @param errorMessage the error message to display if nothing is selected.
     * @param useDefaultComparator set to true to sort the selection list.
     * @param csu when not null, the callback interface to invoke when an item is selected.
     * @param labelProvider the label provider for the combo input.
     * @return
     */
    public <T> ComboViewer createComboViewer(Composite parent,
        String fieldLabel, Collection<? extends T> input, T selection,
        String errorMessage, boolean useDefaultComparator,
        final ComboSelectionUpdate csu, IBaseLabelProvider labelProvider) {
        return createComboViewer(parent, fieldLabel, input, selection,
            errorMessage, useDefaultComparator, null, csu, labelProvider);
    }

    /**
     * Create a combo viewer with a validator when selection is null using errorMessage and using
     * the default comparator if useDefaultComparator is set to true.
     * 
     * @param parent the parent composite.
     * @param fieldLabel the label to be displayed to the left of the combo box.
     * @param input the items to select from.
     * @param selection the default selection.
     * @param errorMessage the error message to display if nothing is selected.
     * @param useDefaultComparator set to true to sort the selection list.
     * @param bindingKey the binding key to be used to enable or disable data binding.
     * @param csu when not null, the callback interface to invoke when an item is selected.
     * @param labelProvider the label provider for the combo input.
     * 
     * @return The combo viewer.
     */
    public <T> ComboViewer createComboViewer(
        Composite parent,
        String fieldLabel,
        Collection<? extends T> input,
        T selection,
        String errorMessage,
        boolean useDefaultComparator,
        String bindingKey,
        final ComboSelectionUpdate csu,
        IBaseLabelProvider labelProvider) {
        Label label = null;
        if (fieldLabel != null)
            label = createLabel(parent, fieldLabel);
        return createComboViewer(parent, label, input, selection, errorMessage,
            useDefaultComparator, bindingKey, csu, labelProvider);
    }

    /**
     * Create a combo using ArrayContentProvider as content provider and BiobankLabelProvider as
     * Label provider.
     */
    public <T> ComboViewer createComboViewer(Composite parent,
        Label fieldLabel, Collection<? extends T> input, T selection,
        String errorMessage, boolean useDefaultComparator, String bindingKey,
        final ComboSelectionUpdate csu, IBaseLabelProvider labelProvider) {

        ReadOnlyComboViewer<T> comboViewer = new ReadOnlyComboViewer<T>(
            parent, dbc, fieldLabel, input, selection, errorMessage, useDefaultComparator,
            csu, labelProvider, modifyListener);

        if (bindingKey != null) {
            bindings.put(bindingKey, comboViewer.getBinding());
        }
        return comboViewer;
    }

    public Label createLabel(Composite parent, String fieldLabel) {
        return createLabel(parent, fieldLabel, SWT.LEFT, true);
    }

    public Label createLabel(Composite parent, String fieldLabel, int style) {
        return createLabel(parent, fieldLabel, style, true);
    }

    @SuppressWarnings("nls")
    public Label createLabel(Composite parent, String fieldLabel, int options,
        boolean addColon) {
        Label label = null;
        String text = fieldLabel;
        if (addColon) {
            text += ":";
        }
        if (toolkit == null) {
            label = new Label(parent, options);
            label.setText(text);
        } else {
            label = toolkit.createLabel(parent, text, options);
        }
        return label;
    }

    public DateTimeWidget createDateTimeWidget(Composite client,
        String nameLabel, Date date, IObservableValue modelObservableValue,
        AbstractValidator validator) {
        return createDateTimeWidget(client, nameLabel, date,
            modelObservableValue, validator, null);
    }

    public DateTimeWidget createDateTimeWidget(Composite client,
        String nameLabel, Date date, IObservableValue modelObservableValue,
        AbstractValidator validator, String bindingKey) {
        return createDateTimeWidget(client, nameLabel, date,
            modelObservableValue, validator, SWT.DATE | SWT.TIME, bindingKey);
    }

    public DateTimeWidget createDateTimeWidget(Composite client,
        String nameLabel, Date date, IObservableValue modelObservableValue,
        AbstractValidator validator, int typeShown) {
        return createDateTimeWidget(client, nameLabel, date,
            modelObservableValue, validator, typeShown, null);
    }

    public DateTimeWidget createDateTimeWidget(Composite client,
        String nameLabel, Date date, IObservableValue modelObservableValue,
        AbstractValidator validator, int typeShown, String bindingKey) {
        Label label = createLabel(client, nameLabel, SWT.NONE, true);
        return createDateTimeWidget(client, label, date, modelObservableValue,
            validator, typeShown, bindingKey);
    }

    public DateTimeWidget createDateTimeWidget(Composite client, Label label,
        Date date, IObservableValue modelObservableValue,
        AbstractValidator validator, int typeShown) {
        return createDateTimeWidget(client, label, date, modelObservableValue,
            validator, typeShown, null);
    }

    public DateTimeWidget createDateTimeWidget(Composite client, Label label,
        Date date, IObservableValue modelObservableValue,
        AbstractValidator validator, int typeShown, String bindingKey) {
        final DateTimeWidget widget = new DateTimeWidget(client, typeShown,
            date);
        if (toolkit != null) {
            widget.adaptToToolkit(toolkit, true);
        }
        if (modelObservableValue != null) {
            UpdateValueStrategy uvs = null;
            if (validator != null) {
                validator.setControlDecoration(BgcBaseWidget.createDecorator(
                    label, validator.getErrorMessage()));
                uvs = new UpdateValueStrategy();
                uvs.setAfterConvertValidator(validator);
            }
            // Same problem than for text. see createText method comment about
            // validators and bindings
            dbc.bindValue(new DateTimeObservableValue(widget),
                modelObservableValue, null, null);
            Binding binding = dbc.bindValue(
                new DateTimeObservableValue(widget), new WritableValue(
                    modelObservableValue.getValue(), Date.class), uvs, null);
            if (bindingKey != null) {
                bindings.put(bindingKey, binding);
            }
        }

        // Don't add a ModifyListener until after the Observable is bound
        // because
        // the binding may cause the value to change, which will call the
        // ModifyListener (not really a modification, it's an initialization).
        if (selectionListener != null) {
            widget.addModifyListener(modifyListener);
        }
        return widget;
    }

    public Binding addBooleanBinding(Control control,
        final WritableValue writableValue,
        final IObservableValue observableValue, final String errorMsg) {

        final ControlDecoration decoration = BgcBaseWidget.createDecorator(
            control, errorMsg, SWT.LEFT | SWT.TOP);
        Binding b = addBooleanBinding(writableValue, observableValue, errorMsg, IStatus.ERROR);

        b.getValidationStatus().addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent event) {
                boolean equal = NullUtil.eq(writableValue.getValue(), observableValue.getValue());

                if (equal) {
                    decoration.hide();
                } else {
                    decoration.show();
                }
            }
        });

        return b;
    }

    public Binding addBooleanBinding(WritableValue writableValue,
        IObservableValue observableValue, final String errorMsg) {
        return addBooleanBinding(writableValue, observableValue, errorMsg, IStatus.ERROR);
    }

    public Binding addBooleanBinding(WritableValue writableValue,
        IObservableValue observableValue, final String errorMsg,
        final int statusType) {
        Assert.isNotNull(dbc);
        UpdateValueStrategy uvs = null;
        if (errorMsg != null) {
            uvs = new UpdateValueStrategy();
            uvs.setAfterConvertValidator(new IValidator() {
                @Override
                public IStatus validate(Object value) {
                    if ((value instanceof Boolean) && !(Boolean) value) {
                        if (statusType == IStatus.WARNING) {
                            return ValidationStatus.warning(errorMsg);
                        }
                        return ValidationStatus.error(errorMsg);
                    }
                    return Status.OK_STATUS;
                }

            });
        }
        return dbc.bindValue(writableValue, observableValue, uvs, uvs);
    }

    public void addGlobalBindValue(IObservableValue statusObservable) {
        Assert.isNotNull(dbc);
        dbc.bindValue(statusObservable,
            new AggregateValidationStatus(dbc.getBindings(),
                AggregateValidationStatus.MAX_SEVERITY));
    }

    protected AbstractValidator createValidator(
        Class<? extends AbstractValidator> validatorClass,
        String validatorErrMsg) {
        try {
            Class<?>[] types = new Class[] { String.class };
            Constructor<?> cons = validatorClass.getConstructor(types);
            Object[] args = new Object[] { validatorErrMsg };
            return (AbstractValidator) cons.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setKeyListener(KeyListener keyListener) {
        this.keyListener = keyListener;
    }

    public void setModifyListener(ModifyListener modifyListener) {
        this.modifyListener = modifyListener;
    }

    public void setSelectionListener(SelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public Binding bindValue(IObservableValue targetObservableValue,
        IObservableValue modelObservableValue,
        UpdateValueStrategy targetToModel, UpdateValueStrategy modelToTarget) {
        Assert.isNotNull(dbc);
        return dbc.bindValue(targetObservableValue, modelObservableValue, targetToModel, modelToTarget);
    }

    public void removeBinding(Binding binding) {
        Assert.isNotNull(dbc);
        dbc.removeBinding(binding);
    }

    public void removeBinding(String bindingKey) {
        Binding binding = bindings.get(bindingKey);
        Assert.isNotNull(binding);
        removeBinding(binding);
    }

    public void addBinding(Binding binding) {
        Assert.isNotNull(dbc);
        dbc.addBinding(binding);
    }

    public void addBinding(String bindingKey) {
        Assert.isNotNull(dbc);
        Binding binding = bindings.get(bindingKey);
        Assert.isNotNull(binding);
        if (!dbc.getBindings().contains(binding)) {
            addBinding(binding);
        }
    }

    public void setBinding(String bindingKey, boolean set) {
        if (set) {
            addBinding(bindingKey);
        } else {
            removeBinding(bindingKey);
        }
    }

    public void createWidgetsFromMap(Map<String, FieldInfo> fieldsMap,
        Composite parent) {
        for (Entry<String, FieldInfo> entry : fieldsMap.entrySet()) {
            FieldInfo fi = entry.getValue();
            Control control = createLabelledWidget(parent, fi.widgetClass,
                SWT.NONE, fi.label, null);
            controls.put(entry.getKey(), control);
        }
    }

    @SuppressWarnings("nls")
    public Control createWidget(Composite parent, Class<?> widgetClass,
        int widgetOptions, String value) {
        if (widgetClass == BgcBaseText.class) {
            if (widgetOptions == SWT.NONE) {
                widgetOptions = SWT.SINGLE;
            }
            BgcBaseText field = createText(parent, widgetOptions | SWT.LEFT,
                null, null);
            if (value != null) {
                field.setText(value);
            }
            return field;
        } else if (widgetClass == Label.class) {
            Label field = createLabel(parent, "", widgetOptions | SWT.LEFT
                | SWT.BORDER, false);
            if (value != null) {
                field.setText(value);
            }
            return field;
        } else if (widgetClass == Button.class) {
            Button button = new Button(parent, SWT.CHECK | widgetOptions);
            button.setEnabled(false);
            toolkit.adapt(button, true, true);
            return button;
        } else {
            Assert.isTrue(false,
                "invalid widget class " + widgetClass.getName());
        }
        return null;
    }

    public Control createLabelledWidget(Composite parent, Class<?> widgetClass,
        int widgetOptions, String fieldLabel, String value) {
        Label label = createLabel(parent, fieldLabel);
        label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        return createWidget(parent, widgetClass, widgetOptions, value);
    }

    public void showWidget(Control widget, boolean show) {
        widget.setVisible(show);
        GridData gd = (GridData) widget.getLayoutData();
        if (gd != null) {
            gd.exclude = !show;
        }
    }

    public void showWidget(Control widget) {
        showWidget(widget, true);
    }

    public void hideWidget(Control widget) {
        showWidget(widget, false);
    }

    public BgcBaseText createReadOnlyLabelledField(Composite parent,
        int widgetOptions, String fieldLabel, String value,
        boolean useBackgroundColor) {
        BgcBaseText widget =
            (BgcBaseText) createLabelledWidget(parent, BgcBaseText.class, SWT.READ_ONLY
                | widgetOptions,
                fieldLabel, value);
        if (useBackgroundColor)
            widget.setBackground(READ_ONLY_TEXT_BGR);
        return widget;
    }

    public BgcBaseText createReadOnlyField(Composite parent, int widgetOptions,
        String value, boolean useBackgroundColor) {
        BgcBaseText widget = (BgcBaseText) createWidget(parent,
            BgcBaseText.class, SWT.READ_ONLY | widgetOptions, value);
        if (useBackgroundColor)
            widget.setBackground(READ_ONLY_TEXT_BGR);
        return widget;
    }

    protected BgcBaseText createReadOnlyLabelledField(Composite parent,
        int widgetOptions, String fieldLabel, String value) {
        return createReadOnlyLabelledField(parent, widgetOptions, fieldLabel,
            value, false);
    }

    public BgcBaseText createReadOnlyLabelledField(Composite parent,
        int widgetOptions, String fieldLabel) {
        return createReadOnlyLabelledField(parent, widgetOptions, fieldLabel,
            null);
    }
}
