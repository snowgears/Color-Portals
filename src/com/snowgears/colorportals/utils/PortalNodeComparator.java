package com.snowgears.colorportals.utils;

import java.util.Comparator;

import com.snowgears.colorportals.Portal;

public class PortalNodeComparator implements Comparator<Portal>{
	@Override
    public int compare(Portal o1, Portal o2) {
        return o1.getNode().compareTo(o2.getNode());
    }
}
