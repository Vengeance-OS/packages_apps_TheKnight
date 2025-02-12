/*
 * Copyright (C) 2019-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.fragments.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.android.ThemeUtils;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import lineageos.preference.LineageSecureSettingSwitchPreference;
import lineageos.providers.LineageSettings;

import org.evolution.settings.preferences.SystemSettingListPreference;
import org.evolution.settings.preferences.SystemSettingSeekBarPreference;
import org.evolution.settings.preferences.SystemSettingSwitchPreference;
import org.evolution.settings.utils.DeviceUtils;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "QuickSettings";

    private static final String KEY_BATTERY_PERCENT = "qs_show_battery_percent";
    private static final String KEY_BATTERY_STYLE = "qs_battery_style";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String KEY_INTERFACE_CATEGORY = "quick_settings_interface_category";
    private static final String KEY_MISCELLANEOUS_CATEGORY = "quick_settings_miscellaneous_category";
    private static final String KEY_QS_BLUETOOTH_SHOW_DIALOG = "qs_bt_show_dialog";
    private static final String KEY_QS_PANEL_STYLE  = "qs_panel_style";
    private static final String KEY_QS_UI_STYLE  = "qs_tile_ui_style";
    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_TILE_ANIM_STYLE = "qs_tile_animation_style";
    private static final String KEY_TILE_ANIM_DURATION = "qs_tile_animation_duration";
    private static final String KEY_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator";

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_STYLE_HIDDEN = 5;

    private PreferenceCategory mInterfaceCategory;
    private PreferenceCategory mMiscellaneousCategory;
    private ListPreference mShowBrightnessSlider;
    private ListPreference mBrightnessSliderPosition;
    private ListPreference mQsPanelStyle;
    private ListPreference mQsUI;
    private LineageSecureSettingSwitchPreference mShowAutoBrightness;
    private SystemSettingListPreference mBatteryStyle;
    private SystemSettingListPreference mBatteryPercent;
    private SystemSettingListPreference mTileAnimationInterpolator;
    private SystemSettingListPreference mTileAnimationStyle;
    private SystemSettingSeekBarPreference mTileAnimationDuration;
    private SystemSettingSwitchPreference mSplitShadePref;

    private static ThemeUtils mThemeUtils;

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.evolution_settings_quick_settings);

        mThemeUtils = ThemeUtils.getInstance(getActivity());

        final Context mContext = getContext();
        final ContentResolver resolver = mContext.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources resources = mContext.getResources();

        mBatteryStyle = (SystemSettingListPreference) findPreference(KEY_BATTERY_STYLE);
        mBatteryPercent = (SystemSettingListPreference) findPreference(KEY_BATTERY_PERCENT);

        int batterystyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);

        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercent.setEnabled(
                batterystyle != BATTERY_STYLE_TEXT && batterystyle != BATTERY_STYLE_HIDDEN);

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        mShowBrightnessSlider.setOnPreferenceChangeListener(this);
        boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

        mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
        mBrightnessSliderPosition.setEnabled(showSlider);

        mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
        boolean automaticAvailable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        if (automaticAvailable) {
            mShowAutoBrightness.setEnabled(showSlider);
        } else {
            prefScreen.removePreference(mShowAutoBrightness);
        }

        mTileAnimationStyle = (SystemSettingListPreference) findPreference(KEY_TILE_ANIM_STYLE);
        mTileAnimationDuration = (SystemSettingSeekBarPreference) findPreference(KEY_TILE_ANIM_DURATION);
        mTileAnimationInterpolator = (SystemSettingListPreference) findPreference(KEY_TILE_ANIM_INTERPOLATOR);
        mTileAnimationStyle.setOnPreferenceChangeListener(this);

        int tileAnimationStyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_TILE_ANIMATION_STYLE, 0, UserHandle.USER_CURRENT);
        updateTileAnimStyle(tileAnimationStyle);

        mMiscellaneousCategory = (PreferenceCategory) findPreference(KEY_MISCELLANEOUS_CATEGORY);

        if (!DeviceUtils.deviceSupportsBluetooth(mContext)) {
            prefScreen.removePreference(mMiscellaneousCategory);
        }

        mQsUI = (ListPreference) findPreference(KEY_QS_UI_STYLE);
        mQsUI.setOnPreferenceChangeListener(this);

        mQsPanelStyle = (ListPreference) findPreference(KEY_QS_PANEL_STYLE);
        mQsPanelStyle.setOnPreferenceChangeListener(this);

        mSplitShadePref = (SystemSettingSwitchPreference) findPreference("qs_split_shade_enabled");
        mSplitShadePref.setOnPreferenceChangeListener(this);

        checkQSOverlays(mContext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            mBrightnessSliderPosition.setEnabled(value > 0);
            if (mShowAutoBrightness != null)
                mShowAutoBrightness.setEnabled(value > 0);
            return true;
        } else if (preference == mBatteryStyle) {
            int value = Integer.parseInt((String) newValue);
            mBatteryPercent.setEnabled(
                    value != BATTERY_STYLE_TEXT && value != BATTERY_STYLE_HIDDEN);
            return true;
        } else if (preference == mQsUI) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_TILE_UI_STYLE, value, UserHandle.USER_CURRENT);
            updateQsStyle(getContext());
            checkQSOverlays(getContext());
            return true;
        } else if (preference == mQsPanelStyle) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_PANEL_STYLE, value, UserHandle.USER_CURRENT);
            updateQsPanelStyle(getContext());
            checkQSOverlays(getContext());
            return true;
        } else if (preference == mTileAnimationStyle) {
            int value = Integer.parseInt((String) newValue);
            updateTileAnimStyle(value);
            return true;
        } else if (preference == mSplitShadePref) {
            int value = (boolean) newValue ? 1 : 0;
            Settings.System.putIntForUser(resolver,
                   "qs_split_shade_enabled", value, UserHandle.USER_CURRENT);
            updateSplitShadeEnabled(getActivity());
            return true;
        }
        return false;
    }

    private void updateTileAnimStyle(int tileAnimationStyle) {
        mTileAnimationDuration.setEnabled(tileAnimationStyle != 0);
        mTileAnimationInterpolator.setEnabled(tileAnimationStyle != 0);
    }

    private void updateSplitShadeEnabled(Context context) {
        ContentResolver resolver = context.getContentResolver();
        boolean splitShadeEnabled = Settings.System.getIntForUser(
                resolver,
                "qs_split_shade_enabled" , 0, UserHandle.USER_CURRENT) != 0;
        String splitShadeStyleCategory = "android.theme.customization.better_qs";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.system.qs.ui.better_qs";
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }
        mHandler.postDelayed(() -> {
            mThemeUtils.setOverlayEnabled(splitShadeStyleCategory, overlayThemeTarget, overlayThemeTarget);
            if (splitShadeEnabled) {
                mThemeUtils.setOverlayEnabled(splitShadeStyleCategory, overlayThemePackage, overlayThemeTarget);
            }
        }, 1250);
    }

    private static void updateQsStyle(Context context) {
        ContentResolver resolver = context.getContentResolver();

        boolean isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE , 0, UserHandle.USER_CURRENT) != 0;

        String qsUIStyleCategory = "android.theme.customization.qs_ui";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.system.qs.ui.A11";

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }

        // reset all overlays before applying
        mThemeUtils.setOverlayEnabled(qsUIStyleCategory, overlayThemeTarget, overlayThemeTarget);

        if (isA11Style) {
            mThemeUtils.setOverlayEnabled(qsUIStyleCategory, overlayThemePackage, overlayThemeTarget);
        }
    }

    private static void updateQsPanelStyle(Context context) {
        ContentResolver resolver = context.getContentResolver();

        int qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);

        String qsPanelStyleCategory = "android.theme.customization.qs_panel";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.systemui";

        switch (qsPanelStyle) {
            case 1:
              overlayThemePackage = "com.android.system.qs.outline";
              break;
            case 2:
            case 3:
              overlayThemePackage = "com.android.system.qs.twotoneaccent";
              break;
            case 4:
              overlayThemePackage = "com.android.system.qs.shaded";
              break;
            case 5:
              overlayThemePackage = "com.android.system.qs.cyberpunk";
              break;
            case 6:
              overlayThemePackage = "com.android.system.qs.neumorph";
              break;
            case 7:
              overlayThemePackage = "com.android.system.qs.reflected";
              break;
            case 8:
              overlayThemePackage = "com.android.system.qs.surround";
              break;
            case 9:
              overlayThemePackage = "com.android.system.qs.thin";
              break;
            default:
              break;
        }

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }

        // reset all overlays before applying
        mThemeUtils.setOverlayEnabled(qsPanelStyleCategory, overlayThemeTarget, overlayThemeTarget);

        if (qsPanelStyle > 0) {
            mThemeUtils.setOverlayEnabled(qsPanelStyleCategory, overlayThemePackage, overlayThemeTarget);
        }
    }

    private void checkQSOverlays(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE , 0, UserHandle.USER_CURRENT);
        int qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

        // Update summaries
        int index = mQsUI.findIndexOfValue(Integer.toString(isA11Style));
        mQsUI.setValue(Integer.toString(isA11Style));
        mQsUI.setSummary(mQsUI.getEntries()[index]);

        index = mQsPanelStyle.findIndexOfValue(Integer.toString(qsPanelStyle));
        mQsPanelStyle.setValue(Integer.toString(qsPanelStyle));
        mQsPanelStyle.setSummary(mQsPanelStyle.getEntries()[index]);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVOLVER;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider(R.xml.evolution_settings_quick_settings) {

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                final Resources resources = context.getResources();

                boolean autoBrightnessAvailable = resources.getBoolean(
                        com.android.internal.R.bool.config_automatic_brightness_available);
                if (!autoBrightnessAvailable) {
                    keys.add(KEY_SHOW_AUTO_BRIGHTNESS);
                }
                if (!DeviceUtils.deviceSupportsBluetooth(context)) {
                    keys.add(KEY_QS_BLUETOOTH_SHOW_DIALOG);
                }
                return keys;
            }
        };
}
