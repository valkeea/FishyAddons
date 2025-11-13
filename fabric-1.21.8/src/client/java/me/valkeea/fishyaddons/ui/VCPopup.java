package me.valkeea.fishyaddons.ui;

import me.valkeea.fishyaddons.ui.widget.VCButton;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Standardized popup dialog with two buttons and an optional text field.
 */
public class VCPopup {
	private final Text title;
	private final String leftButtonText;
	private final String rightButtonText;
	private final boolean hasTextField;
	private final float uiScale;
	private final Runnable onLeft;    
	private java.util.function.Consumer<String> onSave;
	private Runnable onRight;    
	private VCButton.ButtonConfig leftButtonConfig;
	private VCButton.ButtonConfig rightButtonConfig;
	private VCTextField textField;
	private int x;
	private int y;
	private int startY;
	private int endY;	
	private int width;
	private int height;
	private int buttonW;
	private int buttonH;
	private int buttonY;
	private int leftBtnX;
	private int rightBtnX;
	private int tfW;
	private int tfH;
	private int tfX;
	private int tfY;

	/**
	 * Text input + 2 buttons
	 */
	public VCPopup(Text title, Runnable onLeft, String leftButtonText, java.util.function.Consumer<String> onSave, String rightButtonText, float uiScale) {
		this.title = title;
        this.onLeft = onLeft;
		this.leftButtonText = leftButtonText;
		this.rightButtonText = rightButtonText;
		this.hasTextField = true;
		this.onSave = onSave;
		this.uiScale = uiScale;
	}

	/**
	 * Confirm/cancel
	 */
	public VCPopup(Text title, String leftButtonText, Runnable onLeft, String rightButtonText, Runnable onRight, float uiScale) {
		this.title = title;
			this.leftButtonText = leftButtonText;
			this.rightButtonText = rightButtonText;
			this.hasTextField = false;
			this.onSave = null;
			this.onLeft = onLeft;
			this.onRight = onRight;
			this.uiScale = uiScale;
	}    

	private void calcDimensions(float uiScale) {
		width = (int)(260 * uiScale);
		buttonW = (int)(90 * uiScale);
		buttonH = (int)(20 * uiScale);
		buttonY = endY - buttonH - (int)(5 * uiScale);
		tfW = (int)(width - 40 * uiScale);
		tfH = (int)(20 * uiScale);
		tfX = x - tfW / 2;  // Center the text field
		tfY = y + (int)(45 * uiScale);
		
		// Center both buttons as a pair with 5px spacing
		int buttonSpacing = (int)(5 * uiScale);
		int totalButtonWidth = 2 * buttonW + buttonSpacing;
		leftBtnX = x - totalButtonWidth / 2;
		rightBtnX = leftBtnX + buttonW + buttonSpacing;
	}


	public void init(TextRenderer textRenderer, int screenWidth, int screenHeight) {
		height = (int)((hasTextField ? 110 : 80) * uiScale);
		x = screenWidth / 2;
		y = screenHeight / 2 - height / 2;  // Center vertically
		startY = y;
		endY = y + height;
		calcDimensions(uiScale);

		leftButtonConfig = VCButton.standard(
			leftBtnX, buttonY, buttonW, buttonH, leftButtonText)
			.withScale(uiScale);
		rightButtonConfig = VCButton.standard(
			rightBtnX, buttonY, buttonW, buttonH, rightButtonText)
			.withScale(uiScale);

		textField = new VCTextField(textRenderer, tfX, tfY, tfW, tfH, Text.literal(""));
		textField.setUIScale(uiScale);
        textField.setMaxLength(10);
        textField.setFocused(true);
	}

	public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		
		VCRenderUtils.opaqueGradient(context, x - width / 2, y, width, height, 0xFF252525);
		VCRenderUtils.border(context, x - width / 2, y, width, height, 0xFF000000);
		VCText.drawScaledCenteredText(context, textRenderer, title.getString(), x, startY + (int)(10 * uiScale), VCVisuals.getThemeColor(), uiScale);

		if (leftButtonConfig != null && rightButtonConfig != null) {
			leftButtonConfig.withHovered(VCButton.isHovered(
				leftBtnX, buttonY, buttonW, buttonH, mouseX, mouseY));
			rightButtonConfig.withHovered(VCButton.isHovered(
				rightBtnX, buttonY, buttonW, buttonH, mouseX, mouseY));


			VCButton.render(context, textRenderer, leftButtonConfig);
			VCButton.render(context, textRenderer, rightButtonConfig);
			if (hasTextField && textField != null) {
				textField.renderWidget(context, mouseX, mouseY, delta);
			}				

        }
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (hasTextField && textField != null
				&& mouseX >= textField.getX() && mouseX < textField.getX() + textField.getWidth()
				&& mouseY >= textField.getY() && mouseY < textField.getY() + textField.getHeight()
				&& textField.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		return buttonClick(mouseX, mouseY);
	}

    private boolean buttonClick(double mouseX, double mouseY) {
		boolean left = VCButton.isHovered(leftBtnX, buttonY, buttonW, buttonH, (int)mouseX, (int)mouseY);
		boolean right = VCButton.isHovered(rightBtnX, buttonY, buttonW, buttonH, (int)mouseX, (int)mouseY);

		if (left) {
			if (onLeft != null) onLeft.run();
			return true;
		}
		if (right) {
			if (hasTextField) {
				if (onSave != null && textField != null) {
					onSave.accept(textField.getText());
				}
			} else {
				if (onRight != null) onRight.run();
			}
			return true;
		}
		return false;
	}    

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (hasTextField && textField != null) {
			return textField.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}

	public boolean charTyped(char chr, int modifiers) {
		if (hasTextField && textField != null) {
			return textField.charTyped(chr, modifiers);
		}
		return false;
	}

	public int getX() { return x; }
	public int getY() { return y; }
	public int getWidth() { return width; }
	public VCTextField getTextField() { return textField; }
	public boolean hasTextField() { return hasTextField; }
}
