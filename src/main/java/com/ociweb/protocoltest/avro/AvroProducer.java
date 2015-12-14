package com.ociweb.protocoltest.avro;

import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.pipe.util.StreamRegulator;
import com.ociweb.protocoltest.App;
import com.ociweb.protocoltest.data.SequenceExampleA;
import com.ociweb.protocoltest.data.SequenceExampleAFactory;
import com.ociweb.protocoltest.data.build.SequenceExampleAFuzzGenerator;

public class AvroProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AvroProducer.class);

    private final StreamRegulator regulator;
    private final int count;

    public AvroProducer(StreamRegulator regulator, int count) {
        this.regulator = regulator;
        this.count = count;
    }
    
    @Override
    public void run() {
        try {

            OutputStream out = regulator.getOutputStream();
            DataOutputBlobWriter<RawDataSchema> blobWriter = regulator.getBlobWriter();
            long lastNow = 0;

            
            Schema schema = ReflectData.get().getSchema(SequenceExampleA.class);
            
        //    Schema schema = SpecificData.get().getSchema(SequenceExampleA.class);
            //DataFileWriter writer = new DataFileWriter(new SpecificDatumWriter(schema));
            DataFileWriter writer = null;

            
            SequenceExampleAFactory testDataFactory = new SequenceExampleAFuzzGenerator();
            
            
            SequenceExampleA writeMe = testDataFactory.nextObject();            
            int i = count;
            while (i>0) {
                while (regulator.hasRoomForChunk() && --i>=0) { //Note we are only dec when there is room for write
                    lastNow = App.recordSentTime(lastNow, blobWriter);
                    
                    if (null==writer) {
                        writer = new DataFileWriter(new ReflectDatumWriter(schema));
                        writer.create(schema, out);
                    }
                    

                    writer.append(writeMe);
                    writer.flush();

                    writeMe = testDataFactory.nextObject();   
                }
                Thread.yield(); //we are faster than the consumer
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("Avro producer finished");
    }
}