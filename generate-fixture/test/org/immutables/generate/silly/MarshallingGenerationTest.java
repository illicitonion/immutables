package org.immutables.generate.silly;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.collect.ImmutableList;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import org.immutables.common.marshal.Marshaler;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class MarshallingGenerationTest {

  JsonFactory jsonFactory = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
      .disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

  JsonFactory strictierJsonFactory = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

  BsonFactory bsonFactory = new BsonFactory();

  @Test
  public void marshalingIterableMethods() throws IOException {
    Marshaler<SillySubstructure> m = SillySubstructureMarshaler.instance();
    ImmutableList<SillySubstructure> it =
        fromJsonIterable("[{e1:'SOURCE'},{e1:'CLASS'},{e1:'RUNTIME'}]", m);

    check(fromJsonIterable(toJsonIterable(it, m), m)).is(it);
    check(fromBsonIterable(toBsonIterable(it, m), m)).is(it);

    Marshaler<SillyTuplie> m2 = SillyTuplieMarshaler.instance();

    ImmutableList<SillyTuplie> tuplies =
        fromJsonIterable("[[1,null,[]],[2,null,[]]]", m2);

    check(fromJsonIterable(toJsonIterable(tuplies, m2), m2)).is(tuplies);
    check(fromBsonIterable(toBsonIterable(tuplies, m2), m2)).is(tuplies);
  }

  @Test
  public void marshalingPolymorphicTypes() throws IOException {
    Marshaler<SillyPolyHost> m = SillyPolyHostMarshaler.instance();

    SillyPolyHost host = fromJsonIterable("[{ s:[{a:1},{b:'b'},{a:14}] }]", m).get(0);

    check(host.s()).isOf(
        ImmutableSillySub1.builder().a(1).build(),
        ImmutableSillySub2.builder().b("b").build(),
        ImmutableSillySub1.builder().a(14).build());
  }

  @Test
  public void marshalingPolymorphicTypes2() throws IOException {
    Marshaler<SillyPolyHost2> m = SillyPolyHost2Marshaler.instance();

    ImmutableList<SillyPolyHost2> list = fromJsonIterable("[{s:{b:[1,2]} },{s:{b:'b'}}]", m);

    check(list.get(0).s()).is(
        ImmutableSillySub3.builder().addB(1).addB(2).build());
    
    check(list.get(1).s()).is(
        ImmutableSillySub2.builder().b("b").build());
  }

  @Test
  public void marshalingWrapperSingleArgumentTypes() throws IOException {
    Marshaler<SillyIntWrap> m = SillyIntWrapMarshaler.instance();
    ImmutableList<SillyIntWrap> it =
        fromJsonIterable("[123,345,567]", m);

    check(fromJsonIterable(toJsonIterable(it, m), m)).is(it);
    check(fromBsonIterable(toBsonIterable(it, m), m)).is(it);
  }

  @Test
  public void unknownAttributesIgnored() throws IOException {
    Marshaler<SillyMapHolder> m1 = SillyMapHolderMarshaler.instance();

    check(fromJsonIterable("[{ unmolder2:{two:2}, sober:1}]", m1)).hasSize(1);
  }

  @Test
  public void nestedMaps() throws IOException {
    Marshaler<SillyMapTup> m = SillyMapTupMarshaler.instance();

    ImmutableList<SillyMapTup> it =
        fromJsonIterable("[[{'SOURCE':1, 'RUNTIME':2},1]]", m);

    check(fromJsonIterable(toJsonIterable(it, m), m)).is(it);
    check(fromBsonIterable(toBsonIterable(it, m), m)).is(it);

    Marshaler<SillyMapHolder> m1 = SillyMapHolderMarshaler.instance();

    ImmutableList<SillyMapHolder> it1 =
        fromJsonIterable("[{ holder1:{one:1,two:2}, holder2:{'1':'a','3':'b'}, holder3:{'ss':[{'SOURCE':1},1]}}]", m1);

    check(fromJsonIterable(toJsonIterable(it1, m1), m1)).is(it1);
    check(fromBsonIterable(toBsonIterable(it1, m1), m1)).is(it1);
  }

  @Test
  public void marshalAndUnmarshalGeneratedType() throws IOException {
    SillyStructure structure =
        fromJson("{attr1:'x', flag2:false,opt3:1, very4:33, wet5:555.55, subs6:null,"
            + " nest7:{ set2:'METHOD', set3: [1,2,4],floats4:[333.11] },"
            + "int9:0, tup3: [1212.441, null, [true,true,false]]}");

    check(fromJson(toJson(structure))).is(structure);
    check(fromBson(toBson(structure))).is(structure);
  }

  @Test
  public void forceAndSkipEmpty() throws IOException {
    check(marshalDumb(ImmutableSillyDumb.builder().build())).is("{a:null,b:[]}");
    check(marshalDumb(ImmutableSillyDumb.builder().c3(3).addD4("X").build())).is("{a:null,b:[],c:3,d:[\"X\"]}");
  }

  private String marshalDumb(SillyDumb emptyDumb) throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonGenerator jsonGen = jsonFactory.createGenerator(stringWriter);
    SillyDumbMarshaler.marshal(jsonGen, emptyDumb);
    jsonGen.close();
    return stringWriter.toString();
  }

  private <T> ImmutableList<T> fromJsonIterable(String string, Marshaler<T> marshaler) throws IOException {
    JsonParser jsonParser = jsonFactory.createJsonParser(string);
    jsonParser.nextToken();
    return ImmutableList.copyOf(marshaler.unmarshalIterable(jsonParser));
  }

  private <T> String toJsonIterable(ImmutableList<T> it, Marshaler<T> m) throws IOException {
    StringWriter writer = new StringWriter();
    JsonGenerator jsonGen = strictierJsonFactory.createGenerator(writer);
    m.marshalIterable(jsonGen, it);
    jsonGen.close();
    return writer.toString();
  }

  private <T> ImmutableList<T> fromBsonIterable(byte[] bytes, Marshaler<T> marshaler)
      throws IOException {
    JsonParser bsonParser = bsonFactory.createParser(bytes);
    bsonParser.nextToken();
    return ImmutableList.copyOf(marshaler.unmarshalIterable(bsonParser));
  }

  private <T> byte[] toBsonIterable(ImmutableList<T> it, Marshaler<T> marshaler) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BsonGenerator bsonGen = bsonFactory.createJsonGenerator(baos);
    marshaler.marshalIterable(bsonGen, it);
    bsonGen.close();
    return baos.toByteArray();
  }

  private SillyStructure fromJson(String string) throws IOException {
    JsonParser jsonParser = jsonFactory.createJsonParser(string);
    jsonParser.nextToken();
    return SillyStructureMarshaler.instance().unmarshalInstance(jsonParser);
  }

  private SillyStructure fromBson(byte[] bytes) throws IOException {
    JsonParser bsonParser = bsonFactory.createParser(bytes);
    bsonParser.nextToken();
    return SillyStructureMarshaler.instance().unmarshalInstance(bsonParser);
  }

  private String toJson(SillyStructure structure) throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonGenerator jsonGen = jsonFactory.createGenerator(stringWriter);
    SillyStructureMarshaler.marshal(jsonGen, structure);
    jsonGen.close();
    return stringWriter.toString();
  }

  private byte[] toBson(SillyStructure structure) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BsonGenerator bsonGen = bsonFactory.createJsonGenerator(baos);
    SillyStructureMarshaler.marshal(bsonGen, structure);
    bsonGen.close();
    return baos.toByteArray();
  }
}