package com.tymek.fireworks;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private FireworksView fireworksView;
    private TextView hintText;
    private final ArrayList<View> colorSwatches = new ArrayList<>();
    private final ArrayList<Button> typeButtons = new ArrayList<>();
    private boolean autoShowEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fireworksView = findViewById(R.id.fireworks_view);
        hintText = findViewById(R.id.hint_text);

        fireworksView.setOnTouchListener((v, event) -> {
            if (hintText.getVisibility() == View.VISIBLE) {
                hintText.setVisibility(View.GONE);
            }
            return v.onTouchEvent(event);
        });

        buildColorRow();
        buildTypeRow();
        wireActionButtons();
    }

    private void buildColorRow() {
        LinearLayout colorRow = findViewById(R.id.color_row);

        Map<String, Integer> palette = new LinkedHashMap<>();
        palette.put("Czerwony", getColorRes(R.color.spark_red));
        palette.put("Pomarańczowy", getColorRes(R.color.spark_orange));
        palette.put("Żółty", getColorRes(R.color.spark_yellow));
        palette.put("Zielony", getColorRes(R.color.spark_green));
        palette.put("Turkusowy", getColorRes(R.color.spark_cyan));
        palette.put("Niebieski", getColorRes(R.color.spark_blue));
        palette.put("Fioletowy", getColorRes(R.color.spark_purple));

        int index = 0;
        for (Map.Entry<String, Integer> entry : palette.entrySet()) {
            View swatch = createSwatch(entry.getValue(), false);
            final int color = entry.getValue();
            swatch.setContentDescription(entry.getKey());
            swatch.setOnClickListener(v -> {
                fireworksView.setSelectedColor(color);
                selectSwatch(v);
            });
            addToRow(colorRow, swatch, index == 0);
            colorSwatches.add(swatch);
            if (index == 0) {
                fireworksView.setSelectedColor(color);
                markSwatchSelected(swatch);
            }
            index++;
        }

        View rainbowSwatch = createSwatch(Color.WHITE, true);
        rainbowSwatch.setContentDescription(getString(R.string.color_rainbow));
        rainbowSwatch.setOnClickListener(v -> {
            fireworksView.setRainbowMode();
            selectSwatch(v);
        });
        addToRow(colorRow, rainbowSwatch, false);
        colorSwatches.add(rainbowSwatch);
    }

    private View createSwatch(int color, boolean rainbow) {
        View swatch = new View(this);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        if (rainbow) {
            drawable.setColors(new int[]{
                    Color.parseColor("#FF3B30"), Color.parseColor("#FFD60A"),
                    Color.parseColor("#34C759"), Color.parseColor("#2979FF"),
                    Color.parseColor("#BF5AF2")
            });
            drawable.setOrientation(GradientDrawable.Orientation.TL_BR);
        } else {
            drawable.setColor(color);
        }
        swatch.setBackground(drawable);
        swatch.setTag(drawable);
        return swatch;
    }

    private void addToRow(LinearLayout row, View child, boolean firstMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        int margin = dp(4);
        params.setMargins(margin, margin, margin, margin);
        row.addView(child, params);
    }

    private void selectSwatch(View selected) {
        for (View v : colorSwatches) {
            GradientDrawable d = (GradientDrawable) v.getTag();
            d.setStroke(0, Color.TRANSPARENT);
        }
        markSwatchSelected(selected);
    }

    private void markSwatchSelected(View selected) {
        GradientDrawable d = (GradientDrawable) selected.getTag();
        d.setStroke(dp(2), Color.WHITE);
    }

    private void buildTypeRow() {
        LinearLayout typeRow = findViewById(R.id.type_row);

        addTypeButton(typeRow, getString(R.string.type_burst), () -> fireworksView.setSelectedType(FireworksView.BurstType.BURST));
        addTypeButton(typeRow, getString(R.string.type_ring), () -> fireworksView.setSelectedType(FireworksView.BurstType.RING));
        addTypeButton(typeRow, getString(R.string.type_willow), () -> fireworksView.setSelectedType(FireworksView.BurstType.WILLOW));
        addTypeButton(typeRow, getString(R.string.type_palm), () -> fireworksView.setSelectedType(FireworksView.BurstType.PALM));
        addTypeButton(typeRow, getString(R.string.type_crossette), () -> fireworksView.setSelectedType(FireworksView.BurstType.CROSSETTE));
        addTypeButton(typeRow, getString(R.string.type_strobe), () -> fireworksView.setSelectedType(FireworksView.BurstType.STROBE));
        addTypeButton(typeRow, getString(R.string.type_double_ring), () -> fireworksView.setSelectedType(FireworksView.BurstType.DOUBLE_RING));
        addTypeButton(typeRow, getString(R.string.type_random), () -> fireworksView.setRandomType());

        if (!typeButtons.isEmpty()) {
            selectTypeButton(typeButtons.get(0));
        }
    }

    private interface TypeAction {
        void run();
    }

    private void addTypeButton(LinearLayout row, String label, TypeAction action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(getColorRes(R.color.text_primary));
        button.setAllCaps(false);
        button.setTextSize(12f);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setBackground(getDrawable(R.drawable.bg_button));
        button.setOnClickListener(v -> {
            action.run();
            selectTypeButton(button);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = dp(4);
        params.setMargins(margin, 0, margin, 0);
        row.addView(button, params);
        typeButtons.add(button);
    }

    private void selectTypeButton(Button selected) {
        for (Button b : typeButtons) {
            b.setBackground(getDrawable(R.drawable.bg_button));
            b.setTextColor(getColorRes(R.color.text_primary));
        }
        selected.setBackground(getDrawable(R.drawable.bg_button_selected));
        selected.setTextColor(getColorRes(R.color.sky_black));
    }

    private void wireActionButtons() {
        Button clearButton = findViewById(R.id.btn_clear);
        Button autoButton = findViewById(R.id.btn_auto);

        clearButton.setOnClickListener(v -> fireworksView.clearAll());

        autoButton.setOnClickListener(v -> {
            autoShowEnabled = !autoShowEnabled;
            fireworksView.setAutoShow(autoShowEnabled);
            autoButton.setText(autoShowEnabled ? getString(R.string.action_auto_on) : getString(R.string.action_auto_off));
            autoButton.setBackground(getDrawable(autoShowEnabled ? R.drawable.bg_button_selected : R.drawable.bg_button));
            autoButton.setTextColor(getColorRes(autoShowEnabled ? R.color.sky_black : R.color.text_primary));
            if (autoShowEnabled) {
                fireworksView.invalidate();
            }
        });
    }

    private int getColorRes(int resId) {
        return androidx.core.content.ContextCompat.getColor(this, resId);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
