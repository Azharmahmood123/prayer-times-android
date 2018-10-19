/*
 * Copyright (c) 2013-2017 Metin Kale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metinkale.prayer.compass._3D;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.metinkale.prayer.compass.CompassFragment;
import com.metinkale.prayer.compass.CompassFragment.MyCompassListener;
import com.metinkale.prayer.compass.R;

public class Frag3D extends Fragment implements MyCompassListener {

    private CompassView mCompassView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bdl) {
        View v = inflater.inflate(R.layout.compass_3d, container, false);
        mCompassView = v.findViewById(R.id.compass);
        onUpdateDirection();
        return v;

    }

    @Override
    public void onUpdateDirection() {
        if (mCompassView != null) {
            mCompassView.setQibla(((CompassFragment) getParentFragment()).getQiblaAngle(), ((CompassFragment) getParentFragment()).getDistance());
        }

    }

    @Override
    public void onUpdateSensors(float[] rot) {
        if (mCompassView != null) {
            mCompassView.setAngle(rot[0], rot[1], rot[2]);
        }
    }


}
