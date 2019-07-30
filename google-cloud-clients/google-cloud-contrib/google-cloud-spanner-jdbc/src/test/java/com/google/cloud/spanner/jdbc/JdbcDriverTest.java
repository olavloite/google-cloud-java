/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.jdbc;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JdbcDriverTest {

  /**
   * Make sure the JDBC driver class is loaded. This is needed when running the test using Maven.
   */
  static {
    try {
      Class.forName("com.google.cloud.spanner.jdbc.JdbcDriver");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "JdbcDataSource failed to load com.google.cloud.spanner.jdbc.JdbcDriver", e);
    }
  }

  @Test
  public void testConnect() throws SQLException {
    try (Connection connection = DriverManager.getConnection(
        "jdbc:cloudspanner:/projects/test-project/instances/static-test-instance/databases/test-database;credentials=/path/to/key.json")) {
      assertThat(connection.isClosed(), is(false));
    }
  }

  @Test(expected = SQLException.class)
  public void testInvalidConnect() throws SQLException {
    try (Connection connection = DriverManager.getConnection(
        "jdbc:cloudspanner:/projects/test-project/instances/static-test-instance/databases/test-database;credentialsUrl=/path/to/key.json")) {
      assertThat(connection.isClosed(), is(false));
    }
  }

}
