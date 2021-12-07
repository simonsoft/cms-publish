package se.simonsoft.cms.publish.rest.cdn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;

import org.junit.BeforeClass;
import org.junit.Test;

public class PublishCdnUrlSignerCloudFrontTest {

	private static String keyPem = "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCoB7xmCHH/LYeW\n"
			+ "41CC/H4hwXdbokj2XPhHgB1klMmES06W/sOxGChqUlu+tTW05IZj3iP/Alhh3a+K\n"
			+ "HYm8RV6CopCNOcKd00Q0EZBzmN299xhpsRt0vIY7aC4q+iOufKwQRXf+oOHJXY7n\n"
			+ "jJI4K+NIvcewXsqBSRyEetl/mZ5IU7Pdccs+dh4crzJed396OWQKB+pujUuNo2MO\n"
			+ "AZawJd1PKjySHY2/ZTOGYuAqNdk00c2rN5sGUWq3udIJRXFv328wAKmvxWcDp+Zz\n"
			+ "6bo/k6uTCdvAt25GNPuJFkNwu0Lln+rh7l6l3cDULffow6ZGP/VbGHE5ubThi0mp\n"
			+ "7L/2BSyxAgMBAAECggEAL2N3I8OP+uqpScm0JCz3maeJdQNw1mJj9y5Pm9VGfBQN\n"
			+ "BxK0uBZbF5lAnKlYizrEGBhHSJ0ttfQilK498SYTQH5+jAXVOFOj8ZdeiOt85h1w\n"
			+ "+iXj22lr5gc8tgwz0fsBP9mHvP08kGDdc7o2Zrch85rqXXhuXkXzT/2hp5X4iOb1\n"
			+ "JUluEDOQMyhta+sGFAc450FHy0of2mHj2Q4Gbnzt7JArZlDqDTdNrt4bSI4xJsuK\n"
			+ "AyBW7bC+JP4is7+Q19S6TtSZCSau+AFEgCAVU1IGS1IakNxsu3zpoulBPRBoAcB1\n"
			+ "rzcBXK6k7T8ywwGwX0BuMyIxg3eO4j0I4Fg8e+lpnQKBgQDcwLrq+BMlxCq/gJuK\n"
			+ "iHYsYsIQu7pM3pd5MgIR0uNwo1JaCP4Lr8ssiT5haRRufVw5XgoXJl0b00wbWyH9\n"
			+ "eiBTwm6smORJ/ekPkSaEa9K4hlOPRB1r193RhSua0u8B6hMiSgXLu/zyW2V6RYPO\n"
			+ "I0iIVB9k1FTx5anpd/AUZ+NnEwKBgQDC2/gGN30SpuAMXh+FGbCIr7umFVu2OXVz\n"
			+ "nivDy03b/ssEq6l7d1185XYEePLa9jhyEUh3syNxpaPcFA+jcrLKxLgmFyyR0JHj\n"
			+ "JuY0U2G+93zOgKHaw6MecEx/OftUt6z65zUQBCcP4C0IKw/RsmfbyEso0nc/TzTd\n"
			+ "iaG+WS7BqwKBgQCADG+gIlwA+SQOx0vx9KwPnQ2S5UHwmuFkVKSssBZr3ODFBxhN\n"
			+ "sR/6anW7zVcjrCXVxP23ZfJ2VH9+EsPJo5Ci5VKLXXh2jwkklX6xK1Yk4Q4ROWKT\n"
			+ "jrjFcyjS+u3Wv29v4V4xSo80Cd34KeCFryPAqIM5Wo0Xb6+6lG63d3eJ7wKBgQDA\n"
			+ "i8nWosgFQT5NGki9Jfhp6HRdFefM9ZQYjigizeb+xxPnZpUPepC3lKn8m2MmeHyo\n"
			+ "QmnAVhRk/U8gbfZSBUmk0aRBh060O0udENgSxn3kzPrXtwW4fO7XahI8+ZdfTCwy\n"
			+ "tXqae+/5YQQ+eQalqEu2QoH6MZZBycusSY6437kzBwKBgBozsYC1e4GQGKU/LMNf\n"
			+ "HhYPOoMasY1v2cATss9jQB+dpFwb0qaEV1RHoRftgS7KHZw9Gs2VfEQUoXNFX6gB\n"
			+ "u5T2p6pyJwb19qweotgc9prnm0a09mClbD3J5/heDRZOH/LuOarYN2/upFvEiKW0\n"
			+ "1bWCxs5ZfwIKeBk8/FsFgfGD\n"
			+ "-----END PRIVATE KEY-----";
	
	
	private static PublishCdnUrlSignerCloudFront signer;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		signer = new PublishCdnUrlSignerCloudFront(new PublishCdnConfigBase() {
			
			@Override
			public PrivateKey getPrivateKey(String cdn) {
				try {
					return readPrivateKey(keyPem);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			
			@Override
			public String getPrivateKeyId(String cdn) {
				return "K1KPJ6JE57LGCO";
			}
			
			@Override
			public String getHostname(String cdn) {
				return "demo-dev.preview.simonsoftcdn.com";
			}
		});
	}


	
	
	@Test
	public void testGetSignedUrlDocument() throws MalformedURLException {
		
		String urlSigned = signer.getSignedUrlDocument("preview", "/en-GB/SimonsoftCMS-User-manual/latest/WhatsNewIn-D2810D06.html");
		URL url = new URL(urlSigned);
		assertEquals("demo-dev.preview.simonsoftcdn.com", url.getHost());
		assertEquals("/en-GB/SimonsoftCMS-User-manual/latest/WhatsNewIn-D2810D06.html", url.getPath());
		String[] query = url.getQuery().split("&");
		assertEquals(3, query.length);
		
		//assertEquals("", urlSigned);
	}

}
