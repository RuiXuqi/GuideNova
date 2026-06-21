package guideme.internal.util.config;

import java.util.Locale;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MouseWheelSensitivityEntry extends GuiConfigEntries.ButtonEntry {
    private static final int MIN_LOG_STEP = -200;
    private static final int MAX_LOG_STEP = 100;
    private static final double MIN_VALUE = logMouse(MIN_LOG_STEP);
    private static final double MAX_VALUE = logMouse(MAX_LOG_STEP);

    private final double beforeValue;

    public MouseWheelSensitivityEntry(
            GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
        // {@link net.minecraftforge.fml.client.config.GuiConfigEntries.NumberSliderEntry}
        super(owningScreen, owningEntryList, configElement, new MouseWheelSensitivitySlider(
                0,
                owningEntryList.controlX,
                0,
                owningEntryList.controlWidth,
                18,
                readValue(configElement.get())));
        this.beforeValue = readValue(configElement.get());
        this.btnValue.enabled = this.enabled();
        this.updateValueButtonText();
    }

    @Override
    public void updateValueButtonText() {
        this.slider().updateDisplayString();
    }

    @Override
    public void valueButtonPressed(int slotIndex) {
    }

    @Override
    public boolean isDefault() {
        return Double.compare(this.slider().getValue(), readValue(this.configElement.getDefault())) == 0;
    }

    @Override
    public void setToDefault() {
        if (this.enabled()) {
            this.slider().setValue(readValue(this.configElement.getDefault()));
            this.updateValueButtonText();
        }
    }

    @Override
    public boolean isChanged() {
        return Double.compare(this.slider().getValue(), this.beforeValue) != 0;
    }

    @Override
    public void undoChanges() {
        if (this.enabled()) {
            this.slider().setValue(this.beforeValue);
            this.updateValueButtonText();
        }
    }

    @Override
    public boolean saveConfigElement() {
        if (this.enabled() && this.isChanged()) {
            this.configElement.set(this.slider().getValue());
            return this.configElement.requiresMcRestart();
        }
        return false;
    }

    @Override
    public Double getCurrentValue() {
        return this.slider().getValue();
    }

    @Override
    public Double[] getCurrentValues() {
        return new Double[] { this.getCurrentValue() };
    }

    private MouseWheelSensitivitySlider slider() {
        return (MouseWheelSensitivitySlider) this.btnValue;
    }

    private static double readValue(Object value) {
        return sanitize(Double.parseDouble(String.valueOf(value)));
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value)) {
            return logMouse(0);
        }
        return MathHelper.clamp(value, MIN_VALUE, MAX_VALUE);
    }

    private static double toSliderValue(double value) {
        return MathHelper.clamp((unlogMouse(value) - MIN_LOG_STEP) / (double) (MAX_LOG_STEP - MIN_LOG_STEP), 0.0D,
                1.0D);
    }

    private static double logMouse(int value) {
        return Math.pow(10.0D, value / 100.0D);
    }

    private static int unlogMouse(double value) {
        return MathHelper.floor(Math.log10(sanitize(value)) * 100.0D);
    }

    private static final class MouseWheelSensitivitySlider extends GuiSlider {
        private double currentValue;

        private MouseWheelSensitivitySlider(int id, int x, int y, int width, int height, double currentValue) {
            super(id, x, y, width, height, "", "", MIN_LOG_STEP, MAX_LOG_STEP, unlogMouse(currentValue), false, true);
            this.currentValue = sanitize(currentValue);
            this.updateDisplayString();
        }

        @Override
        public void updateSlider() {
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
            this.currentValue = logMouse(this.getLogStep());
            this.updateDisplayString();

            if (this.parent != null) {
                this.parent.onChangeSliderValue(this);
            }
        }

        @Override
        public double getValue() {
            return this.currentValue;
        }

        @Override
        public void setValue(double value) {
            this.currentValue = sanitize(value);
            this.sliderValue = toSliderValue(this.currentValue);
            this.updateDisplayString();
        }

        private void updateDisplayString() {
            this.displayString = String.format(Locale.ROOT, "%.2f", this.currentValue);
        }

        private int getLogStep() {
            return MathHelper.floor(this.minValue + this.sliderValue * (this.maxValue - this.minValue));
        }
    }
}
