package guideme.ui;

public final class UiRect {
    private int x;
    private int y;
    private int width;
    private int height;

    public UiRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public UiRect intersect(UiRect other) {
        int minX = this.x;
        int minY = this.y;
        int maxX = this.x + this.width;
        int maxY = this.y + this.height;
        int oX = other.getX();
        int oY = other.getY();
        int oMaxX = oX + other.getWidth();
        int oMaxY = oY + other.getHeight();
        this.x = Math.max(minX, oX);
        this.y = Math.max(minY, oY);
        this.width = Math.max(0, Math.min(maxX, oMaxX) - this.x);
        this.height = Math.max(0, Math.min(maxY, oMaxY) - this.y);
        return this;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int xPos) {
        this.x = xPos;
    }

    public void setY(int yPos) {
        this.y = yPos;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean contains(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height;
    }
}
