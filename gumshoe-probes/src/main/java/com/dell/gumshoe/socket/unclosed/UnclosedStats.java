package com.dell.gumshoe.socket.unclosed;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.socket.unclosed.SocketCloseMonitor.SocketImplDecorator;
import com.dell.gumshoe.stats.StatisticAdder;

import java.text.MessageFormat;
import java.text.ParseException;

/** thread-safety: toString and fromString are synchronized on FORMAT,
 *      contention not likely in current usage so this is chosen
 *      to reduce overhead of creating lots of new MessageFormat instances.
 *      consider change if use-case is different.
 */
public class UnclosedStats implements StatisticAdder<UnclosedStats> {
    public static final MessageFormat FORMAT =
            new MessageFormat("{0,number,#} sockets, oldest {1,number,#} ms");

    private int count;
    private long maxAge = 0;

    public int getCount() { return count; }
    public long getMaxAge() { return maxAge; }

    public void add(long now, SocketImplDecorator item) {
        this.count++;
        final long age = now - item.openTime;
        this.maxAge = Math.max(this.maxAge, age);
    }

    @Override
    public void add(StatisticAdder<UnclosedStats> value) {
        add(value.get());
    }

    @Override
    public void add(UnclosedStats value) {
        this.count += value.count;
        this.maxAge = Math.max(this.maxAge, value.maxAge);
    }

    @Override
    public UnclosedStats get() {
        return this;
    }

    @Override
    public StatisticAdder<UnclosedStats> newInstance() {
        return new UnclosedStats();
    }

    @Override
    public String toString() {
        synchronized(FORMAT) {
            return FORMAT.format(new Object[] { count, maxAge });
        }
    }

    public static StatisticAdder fromString(String value) {
        final Object[] fields;
        try {
            synchronized(FORMAT) {
                fields = FORMAT.parse(value);
            }
        } catch(Exception e) {
            return null;
        }
        final UnclosedStats out = new UnclosedStats();
        out.count = ((Number)fields[0]).intValue();
        out.maxAge = ((Number)fields[1]).longValue();
        return out;
    }

    @Override
    public String getType() {
        return Probe.UNCLOSED_SOCKET_LABEL;
    }
}