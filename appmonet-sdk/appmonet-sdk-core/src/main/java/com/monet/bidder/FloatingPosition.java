package com.monet.bidder;

import java.util.Map;

class FloatingPosition {
  static final String BOTTOM = "bottom";
  static final String TOP = "top";
  static final String START = "start";
  static final String END = "end";
  static final String HEIGHT = "height";
  static final String WIDTH = "width";
  static final String DP = "dp";
  static final String PERCENT = "percent";

  private final Map<String, Value> positionValues;
  private final String position;

  FloatingPosition(String position, Map<String, Value> positionValues) {
    this.position = position;
    this.positionValues = positionValues;
  }

  static class Value {
    private final int value;
    private final String unit;

    Value(int value, String unit) {
      this.value = value;
      this.unit = unit;
    }

    public int getValue() {
      return value;
    }

    public String getUnit() {
      return unit;
    }
  }
}
