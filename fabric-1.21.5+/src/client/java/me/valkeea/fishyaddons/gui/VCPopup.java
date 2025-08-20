package me.valkeea.fishyaddons.gui;

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
	private int width;
	private int height;

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

	public void init(TextRenderer textRenderer, int screenWidth, int screenHeight) {
		width = (int)(260 * uiScale);
		height = (int)(80 * uiScale);
        height = (hasTextField ? 110 : 80) * (int)(uiScale);
		x = (screenWidth - width) / 2;
		y = (screenHeight - height) / 2;

		int buttonWidth = (int)(90 * uiScale);
		int buttonHeight = (int)(20 * uiScale);
		int buttonY = y + (hasTextField ? 80 : 50) * (int)(uiScale);
		leftButtonConfig = VCButton.standard(x + (int)(15 * uiScale), buttonY, buttonWidth, buttonHeight, leftButtonText)
			.withScale(uiScale);
		rightButtonConfig = VCButton.standard(x + (int)(width - buttonWidth - 15 * uiScale), buttonY, buttonWidth, buttonHeight, rightButtonText)
			.withScale(uiScale);

		int tfWidth = (int)(width - 40 * uiScale);
		int tfHeight = (int)(20 * uiScale);
		int tfX = x + (int)(20 * uiScale);
		int tfY = y + (int)(45 * uiScale);
		textField = new VCTextField(textRenderer, tfX, tfY, tfWidth, tfHeight, Text.literal(""));
		textField.setUIScale(uiScale);
        textField.setMaxLength(10);
        textField.setFocused(true);
	}

	public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
		
		// Render background and title
		me.valkeea.fishyaddons.render.FaLayers.renderAboveOverlay(context, () -> {
			VCRenderUtils.opaqueGradient(context, x, y, width, height, 0xFF252525);
            VCRenderUtils.border(context, x, y, width, height, 0xFF000000);
			VCText.drawScaledCenteredText(context, textRenderer, title.getString(), x + width / 2, y + (int)(15 * uiScale), 0xE2CAE9, uiScale);
		});
			// TextField (if present)
			if (hasTextField && textField != null) {
				textField.renderWidget(context, mouseX, mouseY, delta);
			}

			// Buttons (only if configs are initialized)
			if (leftButtonConfig != null && rightButtonConfig != null) {
				int buttonWidth = (int)(90 * uiScale);
				int buttonHeight = (int)(20 * uiScale);
				int buttonY = y + (hasTextField ? 80 : 50) * (int)(uiScale);
				int leftButtonX = x + (int)(15 * uiScale);
				int rightButtonX = x + (int)(width - buttonWidth - 15 * uiScale);

				leftButtonConfig.withHovered(VCButton.isHovered(
					leftButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY));
				rightButtonConfig.withHovered(VCButton.isHovered(
					rightButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY));

                me.valkeea.fishyaddons.render.FaLayers.renderAboveOverlay(context, () -> {
				VCButton.render(context, textRenderer, leftButtonConfig);
				VCButton.render(context, textRenderer, rightButtonConfig);
			});
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
		int buttonWidth = (int)(90 * uiScale);
		int buttonHeight = (int)(20 * uiScale);
		int buttonY = y + (hasTextField ? 80 : 50) * (int)(uiScale);
		int leftButtonX = x + (int)(15 * uiScale);
		int rightButtonX = x + (int)(width - buttonWidth - 15 * uiScale);

		boolean left = VCButton.isHovered(leftButtonX, buttonY, buttonWidth, buttonHeight, (int)mouseX, (int)mouseY);
		boolean right = VCButton.isHovered(rightButtonX, buttonY, buttonWidth, buttonHeight, (int)mouseX, (int)mouseY);

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
