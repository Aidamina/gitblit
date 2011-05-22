package com.gitblit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

public class MakeCertificate {

	private final static FileSettings fileSettings = new FileSettings();

	public static void main(String... args) {
		Params params = new Params();
		JCommander jc = new JCommander(params);
		try {
			jc.parse(args);
		} catch (ParameterException t) {
			jc.usage();
		}
		File keystore = new File("keystore");
		generateSelfSignedCertificate(params.alias, keystore, params.storePassword, params.subject);
	}
	
	public static void generateSelfSignedCertificate(String hostname, File keystore, String keystorePassword) {
		try {
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

			final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
			
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
			kpGen.initialize(1024, new SecureRandom());
			KeyPair pair = kpGen.generateKeyPair();

			// Generate self-signed certificate
			X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
			builder.addRDN(BCStyle.OU, Constants.NAME);
			builder.addRDN(BCStyle.O, Constants.NAME);
			builder.addRDN(BCStyle.CN, hostname);

			Date notBefore = new Date(System.currentTimeMillis() - 1*24*60*60*1000l);
			Date notAfter = new Date(System.currentTimeMillis() + 10*365*24*60*60*1000l);
			BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(builder.build(), serial, notBefore, notAfter, builder.build(), pair.getPublic());
			ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(pair.getPrivate());
			X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certGen.build(sigGen));
			cert.checkValidity(new Date());
			cert.verify(cert.getPublicKey());

			// Save to keystore			
			KeyStore store = KeyStore.getInstance("JKS");
			if (keystore.exists()) {
				FileInputStream fis = new FileInputStream(keystore);
				store.load(fis, keystorePassword.toCharArray());
			} else {
				store.load(null);
			}
			store.setKeyEntry(hostname, pair.getPrivate(), keystorePassword.toCharArray(), new java.security.cert.Certificate[] { cert });
			store.store(new FileOutputStream(keystore), keystorePassword.toCharArray());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException("Failed to generate self-signed certificate!", t);
		}
	}
	
	public static void generateSelfSignedCertificate(String hostname, File keystore, String keystorePassword, String info) {
		try {
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

			final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
			
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
			kpGen.initialize(1024, new SecureRandom());
			KeyPair pair = kpGen.generateKeyPair();

			// Generate self-signed certificate
			X500Principal principal = new X500Principal(info);
			
			Date notBefore = new Date(System.currentTimeMillis() - 1*24*60*60*1000l);
			Date notAfter = new Date(System.currentTimeMillis() + 10*365*24*60*60*1000l);
			BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(principal, serial, notBefore, notAfter, principal, pair.getPublic());
			ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(pair.getPrivate());
			X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certGen.build(sigGen));
			cert.checkValidity(new Date());
			cert.verify(cert.getPublicKey());

			// Save to keystore			
			KeyStore store = KeyStore.getInstance("JKS");
			if (keystore.exists()) {
				FileInputStream fis = new FileInputStream(keystore);
				store.load(fis, keystorePassword.toCharArray());
			} else {
				store.load(null);
			}
			store.setKeyEntry(hostname, pair.getPrivate(), keystorePassword.toCharArray(), new java.security.cert.Certificate[] { cert });
			store.store(new FileOutputStream(keystore), keystorePassword.toCharArray());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException("Failed to generate self-signed certificate!", t);
		}
	}
	
	@Parameters(separators = " ")
	private static class Params {

		@Parameter(names = { "--alias" }, description = "Server alias", required = true)
		public String alias = null;
		
		@Parameter(names = { "--subject" }, description = "Certificate subject", required = true)
		public String subject = null;
		

		@Parameter(names = "--storePassword", description = "Password for SSL (https) keystore.")
		public String storePassword = fileSettings.getString(Keys.server.storePassword, "");
	}
}
