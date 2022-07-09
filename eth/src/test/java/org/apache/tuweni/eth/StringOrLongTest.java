/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringOrLongTest {

  @Test
  void testEquals() {
    assertTrue(new StringOrLong("3").equals(new StringOrLong("3")));
    assertTrue(new StringOrLong(3L).equals(new StringOrLong(3L)));
    assertFalse(new StringOrLong(3L).equals(new StringOrLong("3")));
  }

  @Test
  void testToStringLong() {
    assertEquals("3", new StringOrLong(3L).toString());
  }

  @Test
  void testToStringString() {
    assertEquals("3", new StringOrLong("3").toString());
  }
}