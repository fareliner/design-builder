/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.design.builder.flattener;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleNameCrawlerListenerTest {

	Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void testGeneratedName1() {

		SimpleNameCrawlerListener l = new SimpleNameCrawlerListener(new File(
				"/tmp"));

		String s = l
				.suggestGeneratedUri(
						"http://services.acme.com/MyService?WSDL&type=XSD&file=schema:f333485f-56bf-4fe6-b33a-8e7744f7b5ab",
						"wsdl");

		log.info("Suggested is {}", s);

		Assert.assertEquals("schema_f333485f-56bf-4fe6-b33a-8e7744f7b5ab.xsd",
				s);

	}

	@Test
	public void testGeneratedName2() {

		SimpleNameCrawlerListener l = new SimpleNameCrawlerListener(new File(
				"/tmp"));

		String s = l
				.suggestGeneratedUri(
						"https://merchantapi.apac.paywithpoli.com/MerchantAPIService.svc?wsdl=wsdl1",
						"wsdl");

		log.info("Suggested is {}", s);

		Assert.assertEquals("MerchantAPIService.svc-wsdl1.wsdl", s);

	}

	@Test
	public void testGeneratedName3() {

		SimpleNameCrawlerListener l = new SimpleNameCrawlerListener(new File(
				"/tmp"));

		String s = l
				.suggestGeneratedUri(
						"https://merchantapi.apac.paywithpoli.com/MerchantAPIService.svc/Xml/transaction/initiate?wsdl",
						"wsdl");

		log.info("Suggested is {}", s);

		Assert.assertEquals("initiate.wsdl", s);

	}

	@Test
	public void testGeneratedName4() {

		SimpleNameCrawlerListener l = new SimpleNameCrawlerListener(new File(
				"/tmp"));

		String s = l
				.suggestGeneratedUri(
						"https://merchantapi.apac.paywithpoli.com/MerchantAPIService.svc?xsd=xsd0",
						"dunno");

		log.info("Suggested is {}", s);

		Assert.assertEquals("MerchantAPIService.svc-xsd0.xsd", s);

	}

	@Test
	public void testGeneratedNameFile() {

		SimpleNameCrawlerListener l = new SimpleNameCrawlerListener(new File(
				"/tmp"));

		String s = l
				.suggestGeneratedUri(
						"file:/C:/dev/workspaces/ndc/model/platform/api/transaction/transaction-session-pool/src/main/resources/SessionPool/SessionPool.wsdl",
						"wsdl");

		log.info("Suggested is {}", s);

		Assert.assertEquals("SessionPool.wsdl", s);

	}

}
