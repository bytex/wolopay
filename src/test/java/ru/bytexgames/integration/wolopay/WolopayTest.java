package ru.bytexgames.integration.wolopay;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Description: </p>
 * Date: 3/3/16 - 12:04 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@baron.su">baron@baron.su</a>
 * @version 1.0.0.0
 */
public class WolopayTest {

	@Test
	public void testUrls() {
		final Wolopay wolopayTest = new Wolopay("clientId", "secret", true, true);
		final Wolopay wolopayProduction = new Wolopay("clientId", "secret", false, true);
		Assert.assertThat(wolopayTest.getEnvironmentURL().toString(), IsEqual.equalTo("https://sandbox.wolopay.com/api/v1/"));
		Assert.assertThat(wolopayProduction.getEnvironmentURL().toString(), IsEqual.equalTo("https://wolopay.com/api/v1/"));
	}

}