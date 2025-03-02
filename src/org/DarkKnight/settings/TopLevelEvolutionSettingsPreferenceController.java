/*
 * Copyright (C) 2019-2024 The TheKnight X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class TopLevelTheKnightSettingsPreferenceController extends BasePreferenceController {

    public TopLevelTheKnightSettingsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
