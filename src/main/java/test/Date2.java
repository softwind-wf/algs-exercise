package test;

public class Date2 extends Date {
    private final int value;

    public Date2(int month, int day, int year) {
        this.value = year * 512 + month * 32 + day;
    }

    @Override
    public int month() {
        return (value / 32) % 16;
    }

    @Override
    public int day() {
        return value % 32;
    }

    @Override
    public int year() {
        return value / 512;
    }

    @Override
    public String toString() {
        return month() + "/" + day() + "/" + year();
    }
}