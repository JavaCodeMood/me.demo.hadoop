package com.lt.hbase.orm.test;

import static com.lt.hbase.orm.test.TestUtil.triplet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.javatuples.Triplet;
import org.junit.Test;

import com.lt.hbase.orm.HBObjectMapper;
import com.lt.hbase.orm.HBRecord;
import com.lt.hbase.orm.Util;
import com.lt.hbase.orm.exceptions.EmptyConstructorInaccessibleException;
import com.lt.hbase.orm.exceptions.FieldsMappedToSameColumnException;
import com.lt.hbase.orm.exceptions.MappedColumnCantBePrimitiveException;
import com.lt.hbase.orm.exceptions.MappedColumnCantBeStaticException;
import com.lt.hbase.orm.exceptions.MappedColumnCantBeTransientException;
import com.lt.hbase.orm.exceptions.MissingHBColumnFieldsException;
import com.lt.hbase.orm.exceptions.MissingHBRowKeyFieldsException;
import com.lt.hbase.orm.exceptions.NoEmptyConstructorException;
import com.lt.hbase.orm.exceptions.ObjectNotInstantiatableException;
import com.lt.hbase.orm.exceptions.RowKeyCantBeComposedException;
import com.lt.hbase.orm.exceptions.RowKeyCantBeEmptyException;
import com.lt.hbase.orm.exceptions.RowKeyCouldNotBeParsedException;
import com.lt.hbase.orm.test.entities.Citizen;
import com.lt.hbase.orm.test.entities.CitizenSummary;
import com.lt.hbase.orm.test.entities.ClassWithBadAnnotationStatic;
import com.lt.hbase.orm.test.entities.ClassWithBadAnnotationTransient;
import com.lt.hbase.orm.test.entities.ClassWithNoEmptyConstructor;
import com.lt.hbase.orm.test.entities.ClassWithNoHBColumns;
import com.lt.hbase.orm.test.entities.ClassWithNoHBRowKeys;
import com.lt.hbase.orm.test.entities.ClassWithPrimitives;
import com.lt.hbase.orm.test.entities.ClassWithTwoFieldsMappedToSameColumn;
import com.lt.hbase.orm.test.entities.Singleton;
import com.lt.hbase.orm.test.entities.UninstantiatableClass;

public class TestHBObjectMapper {

	HBObjectMapper hbMapper = new HBObjectMapper();
	List<Citizen> validObjs = TestObjects.validObjs;

	Result someResult = hbMapper.writeValueAsResult(validObjs.get(0));
	Put somePut = hbMapper.writeValueAsPut(validObjs.get(0));

	@Test
	public void testHBObjectMapper() {
		for (Citizen obj : validObjs) {
			System.out.printf("Original object: %s%n", obj);
			testResult(obj);
			testResultWithRow(obj);
			testPut(obj);
			testPutWithRow(obj);
		}
	}

	public void testResult(HBRecord p) {
		long start, end;
		start = System.currentTimeMillis();
		Result result = hbMapper.writeValueAsResult(p);
		end = System.currentTimeMillis();
		System.out.printf("Time taken for POJO->Result = %dms%n", end - start);
		start = System.currentTimeMillis();
		Citizen pFromResult = hbMapper.readValue(result, Citizen.class);
		end = System.currentTimeMillis();
		assertEquals("Data mismatch after deserialization from Result", p, pFromResult);
		System.out.printf("Time taken for Result->POJO = %dms%n%n", end - start);
	}

	public void testResultWithRow(HBRecord p) {
		long start, end;
		Result result = hbMapper.writeValueAsResult(Arrays.asList(p)).get(0);
		ImmutableBytesWritable rowKey = Util.strToIbw(p.composeRowKey());
		start = System.currentTimeMillis();
		Citizen pFromResult = hbMapper.readValue(rowKey, result, Citizen.class);
		end = System.currentTimeMillis();
		assertEquals("Data mismatch after deserialization from Result+Row", p, pFromResult);
		System.out.printf("Time taken for Result+Row->POJO = %dms%n%n", end - start);
	}

	public void testPut(HBRecord p) {
		long start, end;
		start = System.currentTimeMillis();
		Put put = hbMapper.writeValueAsPut(Arrays.asList(p)).get(0);
		end = System.currentTimeMillis();
		System.out.printf("Time taken for POJO->Put = %dms%n", end - start);
		start = System.currentTimeMillis();
		Citizen pFromPut = hbMapper.readValue(put, Citizen.class);
		end = System.currentTimeMillis();
		assertEquals("Data mismatch after deserialization from Put", p, pFromPut);
		System.out.printf("Time taken for Put->POJO = %dms%n%n", end - start);
	}

	public void testPutWithRow(HBRecord p) {
		long start, end;
		Put put = hbMapper.writeValueAsPut(p);
		ImmutableBytesWritable rowKey = Util.strToIbw(p.composeRowKey());
		start = System.currentTimeMillis();
		Citizen pFromPut = hbMapper.readValue(rowKey, put, Citizen.class);
		end = System.currentTimeMillis();
		assertEquals("Data mismatch after deserialization from Put", p, pFromPut);
		System.out.printf("Time taken for Put->POJO = %dms%n%n", end - start);
	}

	@Test
	public void testInvalidRowKey() {
		Citizen e = TestObjects.validObjs.get(0);
		try {
			hbMapper.readValue("invalid row key", hbMapper.writeValueAsPut(e), Citizen.class);
			fail("Invalid row key should've thrown "
					+ RowKeyCouldNotBeParsedException.class.getName());
		} catch (RowKeyCouldNotBeParsedException ex) {
			System.out
					.println("For a simulate HBase row with invalid row key, below Exception was thrown as expected:\n"
							+ ex.getMessage() + "\n");
		}
	}

	@Test
	public void testValidClasses() {
		assertTrue(hbMapper.isValid(Citizen.class));
		assertTrue(hbMapper.isValid(CitizenSummary.class));
	}

	@Test
	public void testInvalidClasses() {
		List<Triplet<HBRecord, String, Class<? extends IllegalArgumentException>>> invalidRecordsAndErrorMessages = Arrays
				.asList(triplet(Singleton.getInstance(), "A singleton class",
						EmptyConstructorInaccessibleException.class),
						triplet(new ClassWithNoEmptyConstructor(1),
								"Class with no empty constructor",
								NoEmptyConstructorException.class),
						triplet(new ClassWithPrimitives(1f), "A class with primitives",
								MappedColumnCantBePrimitiveException.class),
						triplet(new ClassWithTwoFieldsMappedToSameColumn(),
								"Class with two fields mapped to same column",
								FieldsMappedToSameColumnException.class),
						triplet(new ClassWithBadAnnotationStatic(),
								"Class with a static field mapped to HBase column",
								MappedColumnCantBeStaticException.class),
						triplet(new ClassWithBadAnnotationTransient("James", "Gosling"),
								"Class with a transient field mapped to HBase column",
								MappedColumnCantBeTransientException.class),
						triplet(new ClassWithNoHBColumns(),
								"Class with no fields mapped with HBColumn",
								MissingHBColumnFieldsException.class),
						triplet(new ClassWithNoHBRowKeys(),
								"Class with no fields mapped with HBRowKey",
								MissingHBRowKeyFieldsException.class));
		Set<String> exceptionMessages = new HashSet<String>();
		for (Triplet<HBRecord, String, Class<? extends IllegalArgumentException>> p : invalidRecordsAndErrorMessages) {
			HBRecord record = p.getValue0();
			Class recordClass = record.getClass();
			assertFalse(
					"Object mapper couldn't detect invalidity of class " + recordClass.getName(),
					hbMapper.isValid(recordClass));
			String errorMessage = p.getValue1() + " (" + recordClass.getName()
					+ ") should have thrown an " + IllegalArgumentException.class.getName();
			String exMsgObjToResult = null, exMsgObjToPut = null, exMsgResultToObj = null, exMsgPutToObj = null;
			try {
				hbMapper.writeValueAsResult(record);
				fail(errorMessage + " while converting bean to Result");
			} catch (IllegalArgumentException ex) {
				assertEquals(
						"Mismatch in type of exception thrown for " + recordClass.getSimpleName(),
						p.getValue2(), ex.getClass());
				exMsgObjToResult = ex.getMessage();
			}
			try {
				hbMapper.writeValueAsPut(record);
				fail(errorMessage + " while converting bean to Put");
			} catch (IllegalArgumentException ex) {
				assertEquals(
						"Mismatch in type of exception thrown for " + recordClass.getSimpleName(),
						p.getValue2(), ex.getClass());
				exMsgObjToPut = ex.getMessage();
			}
			try {
				hbMapper.readValue(someResult, recordClass);
				fail(errorMessage + " while converting Result to bean");
			} catch (IllegalArgumentException ex) {
				assertEquals(
						"Mismatch in type of exception thrown for " + recordClass.getSimpleName(),
						p.getValue2(), ex.getClass());
				exMsgResultToObj = ex.getMessage();
			}
			try {
				hbMapper.readValue(new ImmutableBytesWritable(someResult.getRow()), someResult,
						recordClass);
				fail(errorMessage + " while converting Result to bean");
			} catch (IllegalArgumentException ex) {
				assertEquals(
						"Mismatch in type of exception thrown for " + recordClass.getSimpleName(),
						p.getValue2(), ex.getClass());
			}
			try {
				hbMapper.readValue(somePut, recordClass);
				fail(errorMessage + " while converting Put to bean");
			} catch (IllegalArgumentException ex) {
				assertEquals(
						"Mismatch in type of exception thrown for " + recordClass.getSimpleName(),
						p.getValue2(), ex.getClass());
				exMsgPutToObj = ex.getMessage();
			}
			try {
				hbMapper.readValue(new ImmutableBytesWritable(somePut.getRow()), somePut,
						recordClass);
				fail(errorMessage + " while converting row key and Put combo to bean");
			} catch (IllegalArgumentException ex) {
				assertEquals("Mismatch in type of exception thrown", p.getValue2(), ex.getClass());
			}
			assertEquals(
					"Validation for 'conversion to Result' and 'conversion to Put' differ in code path",
					exMsgObjToResult, exMsgObjToPut);
			assertEquals(
					"Validation for 'conversion from Result' and 'conversion from Put' differ in code path",
					exMsgResultToObj, exMsgPutToObj);
			assertEquals(
					"Validation for 'conversion from bean' and 'conversion to bean' differ in code path",
					exMsgObjToResult, exMsgResultToObj);
			System.out.printf("%s threw below Exception as expected:\n%s\n%n", p.getValue1(),
					exMsgObjToResult);
			if (!exceptionMessages.add(exMsgObjToPut)) {
				fail("Same error message for different invalid inputs");
			}
		}
	}

	@Test
	public void testInvalidObjs() {
		for (Triplet<HBRecord, String, Class<? extends IllegalArgumentException>> p : TestObjects.invalidObjs) {
			HBRecord record = p.getValue0();
			String errorMessage = "An object with " + p.getValue1() + " should've thrown an "
					+ IllegalArgumentException.class.getName();
			try {
				hbMapper.writeValueAsResult(record);
				fail(errorMessage + " while converting bean to Result");
			} catch (IllegalArgumentException ex) {
				assertEquals("Mismatch in type of exception thrown", p.getValue2(), ex.getClass());
			}
			try {
				hbMapper.writeValueAsPut(record);
				fail(errorMessage + " while converting bean to Put");
			} catch (IllegalArgumentException ex) {
				assertEquals("Mismatch in type of exception thrown", p.getValue2(), ex.getClass());
			}
		}
	}

	@Test
	public void testEmptyResults() {
		Result nullResult = null, emptyResult = new Result(), resultWithBlankRowKey = new Result();
		// Result nullResult = null, emptyResult = new Result(), resultWithBlankRowKey = new
		// Result(new ImmutableBytesWritable(new byte[]{}));
		Citizen nullCitizen = hbMapper.readValue(nullResult, Citizen.class);
		assertNull("Null Result object should return null", nullCitizen);
		Citizen emptyCitizen = hbMapper.readValue(emptyResult, Citizen.class);
		assertNull("Empty Result object should return null", emptyCitizen);
		assertNull(hbMapper.readValue(resultWithBlankRowKey, Citizen.class));
	}

	@Test
	public void testEmptyPuts() {
		Put nullPut = null, emptyPut = new Put(new byte[] {}), putWithBlankRowKey = new Put(
				new byte[] {});
		// Put nullPut = null, emptyPut = new Put(), putWithBlankRowKey = new Put(new byte[] {});
		Citizen nullCitizen = hbMapper.readValue(nullPut, Citizen.class);
		assertNull("Null Put object should return null", nullCitizen);
		Citizen emptyCitizen = hbMapper.readValue(emptyPut, Citizen.class);
		assertNull("Empty Put object should return null", emptyCitizen);
		assertNull(hbMapper.readValue(putWithBlankRowKey, Citizen.class));
	}

	@Test
	public void testGetRowKey() {
		ImmutableBytesWritable rowKey = hbMapper.getRowKey(new HBRecord() {
			@Override
			public String composeRowKey() {
				return "rowkey";
			}

			@Override
			public void parseRowKey(String rowKey) {

			}
		});
		assertEquals("Row keys don't match", rowKey, Util.strToIbw("rowkey"));
		try {
			hbMapper.getRowKey(new HBRecord() {
				@Override
				public String composeRowKey() {
					return null;
				}

				@Override
				public void parseRowKey(String rowKey) {

				}
			});
			fail("null row key should've thrown a " + RowKeyCantBeEmptyException.class.getName());
		} catch (RowKeyCantBeEmptyException npx) {

		}
		try {
			hbMapper.getRowKey(new HBRecord() {
				@Override
				public String composeRowKey() {
					throw new RuntimeException("Some blah");
				}

				@Override
				public void parseRowKey(String rowKey) {

				}
			});
			fail("If row key can't be composed, an "
					+ RowKeyCantBeComposedException.class.getName() + " was expected");
		} catch (RowKeyCantBeComposedException e) {

		}
		try {
			hbMapper.getRowKey(null);
			fail("If object is null, a " + NullPointerException.class.getName() + " was expected");
		} catch (NullPointerException npx) {

		}
	}

	@Test
	public void testUninstantiatableClass() {
		try {
			hbMapper.readValue(someResult, UninstantiatableClass.class);
			fail("If class can't be instantiated, a "
					+ ObjectNotInstantiatableException.class.getName() + " was expected");
		} catch (ObjectNotInstantiatableException e) {

		}
	}
}
