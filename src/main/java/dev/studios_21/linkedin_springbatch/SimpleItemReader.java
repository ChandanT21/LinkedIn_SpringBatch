package dev.studios_21.linkedin_springbatch;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimpleItemReader implements ItemReader<String> {

    private List<String> dataSet = new ArrayList<>();

    private Iterator<String> iterator;

    public SimpleItemReader() {
        this.dataSet.add("0");
        this.dataSet.add("1");
        this.dataSet.add("2");
        this.dataSet.add("3");
        this.dataSet.add("4");
        this.dataSet.add("5");
        this.dataSet.add("6");
        this.dataSet.add("7");
        this.dataSet.add("8");
        this.dataSet.add("9");
        this.iterator = this.dataSet.iterator();
    }

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
