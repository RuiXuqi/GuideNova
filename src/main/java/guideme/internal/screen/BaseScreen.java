package guideme.internal.screen;

import guideme.internal.GuideMEClient;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Base for mouse handlers in double values, making mouse smooth.
 */
public abstract class BaseScreen extends GuiScreen {
    public static final int LEFT_MOUSE_BUTTON = GLFW.GLFW_MOUSE_BUTTON_LEFT;
    public static final int RIGHT_MOUSE_BUTTON = GLFW.GLFW_MOUSE_BUTTON_RIGHT;

    private static long callbackWindow;
    private static GLFWCursorPosCallbackI cursorPosCallback;
    private static GLFWCursorPosCallbackI previousCursorPosCallback;
    private static GLFWMouseButtonCallbackI mouseButtonCallback;
    private static GLFWMouseButtonCallbackI previousMouseButtonCallback;
    private static GLFWScrollCallbackI scrollCallback;
    private static GLFWScrollCallbackI previousScrollCallback;

    private double mouseX;
    private double mouseY;
    private double lastMouseX;
    private double lastMouseY;
    private int fakeRightMouse;
    private double mousePressedTime;

    @Override
    public void handleMouseInput() {
        // no-op
    }

    @Deprecated
    @Override
    protected final void mouseClicked(int mouseX, int mouseY, int button) {
    }

    @Deprecated
    @Override
    protected final void mouseReleased(int mouseX, int mouseY, int button) {
    }

    @Deprecated
    @Override
    protected final void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
    }

    /**
     * Called when the mouse is moved within the GUI element.
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     */
    protected void mouseMoved(double mouseX, double mouseY) {
    }

    /**
     * Called after {@link #mouseMoved(double, double)} is called.
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     */
    protected void afterMouseMove(double mouseX, double mouseY) {
    }

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was clicked.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    protected boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            super.mouseClicked((int) mouseX, (int) mouseX, button);
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that was released.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    protected boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean pre = this.selectedButton != null;
        super.mouseReleased((int) mouseX, (int) mouseY, button);
        return pre && this.selectedButton == null;
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param button the button that is being dragged.
     * @param dragX  the X distance of the drag.
     * @param dragY  the Y distance of the drag.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    protected boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    /**
     * Called when the mouse wheel is scrolled within the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @param delta  the scrolling delta.
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     */
    protected boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    private void updateMousePosition() {
        if (!Display.isCreated()) {
            this.mouseX = 0;
            this.mouseY = 0;
            return;
        }

        long window = Display.getWindow();
        if (window == 0) {
            this.mouseX = 0;
            this.mouseY = 0;
            return;
        }

        var cursorX = BufferUtils.createDoubleBuffer(1);
        var cursorY = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(window, cursorX, cursorY);

        this.mouseX = cursorX.get(0) * this.width / this.mc.displayWidth;
        this.mouseY = cursorY.get(0) * this.height / this.mc.displayHeight;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public static void installMouseCallbacks() {
        if (!Display.isCreated()) {
            return;
        }

        long window = Display.getWindow();
        if (window == 0 || callbackWindow == window) {
            return;
        }

        if (cursorPosCallback == null) {
            cursorPosCallback = BaseScreen::onGlfwCursorMoved;
        }
        if (mouseButtonCallback == null) {
            mouseButtonCallback = BaseScreen::onGlfwMouseButton;
        }
        if (scrollCallback == null) {
            scrollCallback = BaseScreen::onGlfwScrolled;
        }

        previousCursorPosCallback = GLFW.glfwSetCursorPosCallback(window, cursorPosCallback);
        previousMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window, mouseButtonCallback);
        previousScrollCallback = GLFW.glfwSetScrollCallback(window, scrollCallback);
        callbackWindow = window;
    }

    private static void onGlfwCursorMoved(long window, double x, double y) {
        if (previousCursorPosCallback != null && previousCursorPosCallback != cursorPosCallback) {
            previousCursorPosCallback.invoke(window, x, y);
        }

        var screen = getCurrentScreen(window);
        if (screen == null)
            return;

        double mouseX = x * screen.width / screen.mc.displayWidth;
        double mouseY = y * screen.height / screen.mc.displayHeight;
        screen.mouseX = mouseX;
        screen.mouseY = mouseY;

        screen.mouseMoved(mouseX, mouseY);
        if (screen.eventButton != -1 && screen.mousePressedTime > 0.0) {
            screen.mouseDragged(
                    mouseX, mouseY, screen.eventButton,
                    mouseX - screen.lastMouseX, mouseY - screen.lastMouseY);
        }
        screen.afterMouseMove(mouseX, mouseY);

        screen.lastMouseX = mouseX;
        screen.lastMouseY = mouseY;
    }

    private static void onGlfwMouseButton(long window, int button, int action, int modifiers) {
        if (previousMouseButtonCallback != null && previousMouseButtonCallback != mouseButtonCallback) {
            previousMouseButtonCallback.invoke(window, button, action, modifiers);
        }

        var screen = getCurrentScreen(window);
        if (screen == null)
            return;

        boolean pressed = action == GLFW.GLFW_PRESS;
        if (Minecraft.IS_RUNNING_ON_MAC && button == LEFT_MOUSE_BUTTON) {
            if (pressed) {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL) {
                    button = RIGHT_MOUSE_BUTTON;
                    ++screen.fakeRightMouse;
                }
            } else if (screen.fakeRightMouse > 0) {
                button = RIGHT_MOUSE_BUTTON;
                --screen.fakeRightMouse;
            }
        }

        int mouseButton = button;
        if (pressed) {
            if (screen.mc.gameSettings.touchscreen && screen.touchValue++ > 0) {
                return;
            }

            screen.eventButton = mouseButton;
            screen.mousePressedTime = GLFW.glfwGetTime();
        } else if (screen.eventButton != -1) {
            if (screen.mc.gameSettings.touchscreen && --screen.touchValue > 0) {
                return;
            }

            screen.eventButton = -1;
        }

        screen.updateMousePosition();
        if (pressed) {
            screen.mouseClicked(screen.mouseX, screen.mouseY, mouseButton);
        } else {
            screen.mouseReleased(screen.mouseX, screen.mouseY, mouseButton);
        }
    }

    private static void onGlfwScrolled(long window, double xOffset, double yOffset) {
        if (previousScrollCallback != null && previousScrollCallback != scrollCallback) {
            previousScrollCallback.invoke(window, xOffset, yOffset);
        }

        double offset = yOffset;
        if (Minecraft.IS_RUNNING_ON_MAC && yOffset == 0.0) {
            offset = xOffset;
        }
        if (previousScrollCallback == null) {
            Mouse.addWheelEvent(offset);
        }

        var screen = getCurrentScreen(window);
        if (screen == null)
            return;

        double delta = (GuideMEClient.isDiscreteScrolling() ? Math.signum(offset) : offset)
                * GuideMEClient.getScrollSensitivity();
        if (delta == 0.0)
            return;

        screen.updateMousePosition();
        screen.mouseScrolled(screen.mouseX, screen.mouseY, delta);
    }

    private static BaseScreen getCurrentScreen(long window) {
        return window == Display.getWindow()
                && Minecraft.getMinecraft().currentScreen instanceof BaseScreen screen ? screen : null;
    }
}
