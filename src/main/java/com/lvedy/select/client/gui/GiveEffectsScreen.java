package com.lvedy.select.client.gui;

import com.lvedy.select.network.GiveEffectPacket;
import com.lvedy.select.special.select.SelectEffect;
import com.lvedy.select.special.select.SelectEffects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 给予效果测试界面（/select give）。
 * 增益两行、负面两行，横向滚动查看全部效果。
 * 可多选，选中效果用双圈白+黄高亮，点「给予」一次性发包。
 */
public class GiveEffectsScreen extends Screen {

    // ── 尺寸常量 ──────────────────────────────────────────────────
    private static final int CARD_W = 82;
    private static final int CARD_H  = 38;
    private static final int CARD_GAP = 4;
    private static final int ROW_GAP  = 3;
    private static final int SECTION_GAP = 7;
    private static final int SCROLLBAR_H = 6;
    private static final int TITLE_H  = 14;   // 标题区高度
    private static final int CONFIRM_H = 16;
    private static final int BOTTOM_PAD = 4;  // 确认按钮距屏幕底部

    // 左侧标签列宽（只放"▲"/"▼"两个字符，节省空间）
    private static final int LABEL_W = 10;

    // ── 数据 ──────────────────────────────────────────────────────
    private final List<SelectEffect> buffRow1  = new ArrayList<>();
    private final List<SelectEffect> buffRow2  = new ArrayList<>();
    private final List<SelectEffect> debuffRow1 = new ArrayList<>();
    private final List<SelectEffect> debuffRow2 = new ArrayList<>();

    // 已选效果集合（有序，方便遍历）
    private final Set<SelectEffect> selected = new LinkedHashSet<>();

    // ── 布局 ──────────────────────────────────────────────────────
    private int viewLeft, viewRight;
    private int r1Y, r2Y, r3Y, r4Y;
    private int scrollbarY;

    private double scrollX = 0;
    private double maxScrollX = 0;
    private boolean dragging = false;

    private Button confirmButton;

    public GiveEffectsScreen() {
        super(Component.literal("给予效果"));
    }

    @Override
    protected void init() {
        buffRow1.clear(); buffRow2.clear();
        debuffRow1.clear(); debuffRow2.clear();
        selected.clear();

        List<SelectEffect> ab = new ArrayList<>(), ad = new ArrayList<>();
        for (SelectEffect e : SelectEffects.all()) (e.isBuff() ? ab : ad).add(e);
        split(ab, buffRow1, buffRow2);
        split(ad, debuffRow1, debuffRow2);

        viewLeft  = LABEL_W + 3;
        viewRight = width - 4;

        // 确认按钮 Y（从底部往上推算）
        int confirmY = height - BOTTOM_PAD - CONFIRM_H;
        scrollbarY = confirmY - 4 - SCROLLBAR_H;

        // 四行从顶部往下排，确保不超 scrollbarY
        int totalRows = CARD_H * 4 + ROW_GAP * 2 + SECTION_GAP;
        int availH = scrollbarY - TITLE_H - 2;
        // 如果空间不够，按比例压缩起始 Y；一般 166px GUI 刚好够放下
        r1Y = TITLE_H + Math.max(0, (availH - totalRows) / 2);
        r2Y = r1Y + CARD_H + ROW_GAP;
        r3Y = r2Y + CARD_H + SECTION_GAP;
        r4Y = r3Y + CARD_H + ROW_GAP;

        // 滚动范围
        int maxCols = max4(buffRow1.size(), buffRow2.size(), debuffRow1.size(), debuffRow2.size());
        int contentW = maxCols * CARD_W + Math.max(0, maxCols - 1) * CARD_GAP;
        maxScrollX = Math.max(0, contentW - (viewRight - viewLeft));
        scrollX = Math.min(scrollX, maxScrollX);

        confirmButton = Button.builder(Component.literal("给予"), b -> confirm())
                .pos(width / 2 - 30, confirmY).size(60, CONFIRM_H).build();
        confirmButton.active = false;
        addRenderableWidget(confirmButton);
    }

    private static void split(List<SelectEffect> src, List<SelectEffect> r1, List<SelectEffect> r2) {
        int half = (src.size() + 1) / 2;
        for (int i = 0; i < src.size(); i++) (i < half ? r1 : r2).add(src.get(i));
    }

    private static int max4(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    private void confirm() {
        if (selected.isEmpty()) return;
        List<String> ids = new ArrayList<>();
        for (SelectEffect e : selected) ids.add(e.getId());
        PacketDistributor.sendToServer(new GiveEffectPacket(ids));
    }

    // ─── 渲染 ────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xC0101010);
        g.drawCenteredString(font, "给予效果", width / 2, 4, 0xFFFFFFFF);

        // 左侧区域标签（小字号用单个字符区分）
        int buffMid  = (r1Y + r2Y + CARD_H) / 2 - font.lineHeight / 2;
        int debuffMid = (r3Y + r4Y + CARD_H) / 2 - font.lineHeight / 2;
        g.drawString(font, "+", 1, buffMid,  0xFF55FF55);
        g.drawString(font, "-", 1, debuffMid, 0xFFFF5555);

        // 裁剪内容区
        g.enableScissor(viewLeft, r1Y, viewRight, r4Y + CARD_H);
        drawRow(g, buffRow1,  r1Y,  true,  mx, my);
        drawRow(g, buffRow2,  r2Y,  true,  mx, my);
        drawRow(g, debuffRow1, r3Y, false, mx, my);
        drawRow(g, debuffRow2, r4Y, false, mx, my);
        g.disableScissor();

        drawScrollbar(g);

        for (net.minecraft.client.gui.components.Renderable r : renderables) {
            r.render(g, mx, my, pt);
        }
    }

    private void drawRow(GuiGraphics g, List<SelectEffect> row, int rowY,
                         boolean isBuff, int mx, int my) {
        int x = viewLeft - (int) scrollX;
        for (SelectEffect e : row) {
            boolean hov = mx >= x && mx <= x + CARD_W && my >= rowY && my <= rowY + CARD_H;
            boolean sel = selected.contains(e);
            drawCard(g, x, rowY, e, isBuff, hov, sel);
            x += CARD_W + CARD_GAP;
        }
    }

    private void drawCard(GuiGraphics g, int x, int y, SelectEffect e,
                          boolean isBuff, boolean hov, boolean sel) {
        g.fill(x, y, x + CARD_W, y + CARD_H, hov ? 0xEE303030 : 0xCC000000);

        // 选中：双圈白+黄（与 SelectionScreen 一致）
        if (sel) {
            g.renderOutline(x - 2, y - 2, CARD_W + 4, CARD_H + 4, 0xFFFFFFFF);
            g.renderOutline(x - 1, y - 1, CARD_W + 2, CARD_H + 2, 0xFFFFFF00);
        }
        g.renderOutline(x, y, CARD_W, CARD_H, isBuff ? 0xFF55FF55 : 0xFFFF5555);

        int tc = sel ? 0xFFFFFF00 : (isBuff ? 0xFF55FF55 : 0xFFFF5555);
        g.drawString(font, e.getDisplayName(), x + 3, y + 2, tc);

        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.literal(e.getDescription()), CARD_W - 6);
        int ty = y + 2 + font.lineHeight + 1;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (ty + font.lineHeight > y + CARD_H) break;
            g.drawString(font, line, x + 3, ty, 0xFFBBBBBB);
            ty += font.lineHeight;
        }
    }

    private void drawScrollbar(GuiGraphics g) {
        int tw = viewRight - viewLeft;
        g.fill(viewLeft, scrollbarY, viewRight, scrollbarY + SCROLLBAR_H, 0xFF333333);
        if (maxScrollX <= 0) {
            g.fill(viewLeft, scrollbarY, viewRight, scrollbarY + SCROLLBAR_H, 0xFF666666);
            return;
        }
        int cw = tw + (int) maxScrollX;
        int hw = Math.max(16, (int) ((float) tw * tw / cw));
        int hx = viewLeft + (int) ((scrollX / maxScrollX) * (tw - hw));
        g.fill(hx, scrollbarY, hx + hw, scrollbarY + SCROLLBAR_H, 0xFFAAAAAA);
    }

    // ─── 交互 ────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (my >= scrollbarY && my <= scrollbarY + SCROLLBAR_H
                    && mx >= viewLeft && mx <= viewRight) {
                dragging = true;
                setScrollFromMouse(mx);
                return true;
            }
            SelectEffect hit = hitTest(mx, my);
            if (hit != null) {
                if (!selected.remove(hit)) selected.add(hit); // 切换选中
                confirmButton.active = !selected.isEmpty();
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private SelectEffect hitTest(double mx, double my) {
        if (mx < viewLeft || mx > viewRight) return null;
        SelectEffect e;
        if ((e = hitRow(buffRow1,  r1Y,  mx, my)) != null) return e;
        if ((e = hitRow(buffRow2,  r2Y,  mx, my)) != null) return e;
        if ((e = hitRow(debuffRow1, r3Y, mx, my)) != null) return e;
        if ((e = hitRow(debuffRow2, r4Y, mx, my)) != null) return e;
        return null;
    }

    private SelectEffect hitRow(List<SelectEffect> row, int rowY, double mx, double my) {
        if (my < rowY || my > rowY + CARD_H) return null;
        int x = viewLeft - (int) scrollX;
        for (SelectEffect e : row) {
            if (mx >= x && mx <= x + CARD_W) return e;
            x += CARD_W + CARD_GAP;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) { setScrollFromMouse(mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sdx, double sdy) {
        if (maxScrollX > 0) {
            scrollX = Math.max(0, Math.min(maxScrollX, scrollX - sdy * 16));
            return true;
        }
        return super.mouseScrolled(mx, my, sdx, sdy);
    }

    private void setScrollFromMouse(double mx) {
        int tw = viewRight - viewLeft;
        scrollX = Math.max(0, Math.min(maxScrollX, ((mx - viewLeft) / tw) * maxScrollX));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
