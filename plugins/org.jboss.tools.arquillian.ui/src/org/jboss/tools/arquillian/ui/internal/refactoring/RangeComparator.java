package org.jboss.tools.arquillian.ui.internal.refactoring;

import org.eclipse.compare.rangedifferencer.IRangeComparator;

public class RangeComparator implements IRangeComparator {
    private final String value;

    public RangeComparator(String string) {
      value = string;
    }

    @Override
    public int getRangeCount() {
      return value.length();
    }

    @Override
    public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
      RangeComparator otherComparator = (RangeComparator) other;
      return value.charAt(thisIndex) == otherComparator.value.charAt(otherIndex);
    }

    @Override
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
      return false;
    }
  }