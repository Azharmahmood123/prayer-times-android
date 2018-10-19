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

package com.metinkale.prayer.calendar;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.metinkale.prayer.BaseActivity;
import com.metinkale.prayer.HijriDate;
import com.metinkale.prayer.utils.Utils;

import org.joda.time.LocalDate;

import java.util.Locale;


public class CalendarFragment extends BaseActivity.MainFragment implements OnItemClickListener {
    private static final String[] ASSETS = {"/dinigunler/hicriyil.html", "/dinigunler/asure.html", "/dinigunler/mevlid.html", "/dinigunler/3aylar.html", "/dinigunler/regaib.html", "/dinigunler/mirac.html", "/dinigunler/berat.html", "/dinigunler/ramazan.html", "/dinigunler/kadir.html", "/dinigunler/arefe.html", "/dinigunler/ramazanbay.html", "/dinigunler/ramazanbay.html", "/dinigunler/ramazanbay.html", "/dinigunler/arefe.html", "/dinigunler/kurban.html", "/dinigunler/kurban.html", "/dinigunler/kurban.html", "/dinigunler/kurban.html"};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.calendar_main, container, false);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        ViewPager viewPager = v.findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(LocalDate.now().getYear() - HijriDate.getMIN_YEAR());
        return v;
    }

    @Nullable
    public static String getAssetForHolyday(int pos) {
        return Utils.getLanguage("en", "de", "tr") + ASSETS[pos - 1];
    }



    @Override
    public void onItemClick(AdapterView<?> arg0, @NonNull View v, int pos, long arg3) {
        String asset = getAssetForHolyday((Integer) v.getTag());

        Locale lang = Utils.getLocale();
        if ((asset != null) && (new Locale("de").getLanguage().equals(lang.getLanguage())
                || new Locale("tr").getLanguage().equals(lang.getLanguage()))) {
            Bundle bdl = new Bundle();
            bdl.putString("asset", asset);
            Fragment frag = new WebViewFragment();
            frag.setArguments(bdl);
            moveToFrag(frag);
        }
    }

    public static class YearFragment extends Fragment {

        public static final String YEAR = "year";
        private int year;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.calendar_frag, container, false);

            ListView listView = view.findViewById(android.R.id.list);

            year = getArguments().getInt(YEAR);
            listView.setAdapter(new Adapter(getActivity(), year));

            listView.setOnItemClickListener((OnItemClickListener) getParentFragment());

            return view;

        }


    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (Utils.getLocale().getLanguage().equals(new Locale("ar").getLanguage()))
                position = getCount() - position - 1;
            Fragment fragment = new YearFragment();
            Bundle args = new Bundle();
            args.putInt(YearFragment.YEAR, position + HijriDate.getMIN_YEAR());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return HijriDate.getMAX_YEAR() - HijriDate.getMIN_YEAR();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (Utils.getLocale().getLanguage().equals(new Locale("ar").getLanguage()))
                position = getCount() - position - 1;
            return Utils.toArabicNrs(position + HijriDate.getMIN_YEAR());
        }
    }

}
