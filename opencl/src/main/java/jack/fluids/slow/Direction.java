package jack.fluids.slow;

public enum Direction {
  NORTH {
    @Override
    int toFacei(int i) {
      return (i + 1) * 2;
    }
  },
  SOUTH {
    @Override
    int toFacei(int i) {
      return i * 2;
    }
  },
  EAST {
    @Override
    int toFacej(int j) {
      return (j + 1) * 2;
    }
  },
  WEST {
    @Override
    int toFacej(int j) {
      return j * 2;
    }
  };

  int toFacei(int i) {
    return i;
  }

  int toFacej(int j) {
    return j;
  }
}
