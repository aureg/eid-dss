/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.dss.portal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SignatureResponseProcessorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SignatureResponseProcessorServlet.class);

	public static final String SIGNATURE_RESPONSE_PARAMETER = "SignatureResponse";

	public static final String NEXT_PAGE_INIT_PARAM = "NextPage";

	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE = SignatureResponseProcessorServlet.class
			.getName()
			+ ".signedDocument";

	private String nextPage;

	@Override
	public void init(ServletConfig config) throws ServletException {
		LOG.debug("init");
		this.nextPage = config.getInitParameter(NEXT_PAGE_INIT_PARAM);
		if (null == this.nextPage) {
			throw new ServletException("missing init-param: "
					+ NEXT_PAGE_INIT_PARAM);
		}
		LOG.debug("next page: " + this.nextPage);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doPost");
		String signatureResponse = request
				.getParameter(SIGNATURE_RESPONSE_PARAMETER);
		if (null == signatureResponse) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, response);
			return;
		}
		byte[] decodedSignatureResponse = Base64.decode(signatureResponse);
		LOG.debug("decoded signature response: "
				+ new String(decodedSignatureResponse));
		try {
			loadDocument(new ByteArrayInputStream(decodedSignatureResponse));
		} catch (Exception e) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " is not an XML document";
			LOG.error(msg);
			showErrorPage(msg, response);
			return;
		}

		HttpSession httpSession = request.getSession();
		setSignedDocument(new String(decodedSignatureResponse), httpSession);

		response.sendRedirect(this.nextPage);
	}

	private void setSignedDocument(String signedDocument,
			HttpSession httpSession) {
		httpSession.setAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE,
				signedDocument);
	}

	public static String getSignedDocument(HttpSession httpSession) {
		String signedDocument = (String) httpSession
				.getAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE);
		return signedDocument;
	}

	private Document loadDocument(InputStream documentInputStream)
			throws ParserConfigurationException, SAXException, IOException {
		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.parse(inputSource);
		return document;
	}

	private void showErrorPage(String message, HttpServletResponse response)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out
				.println("<head><title>eID DSS Signature Response Processor</title></head>");
		out.println("<body>");
		out.println("<h1>eID DSS Signature Response Processor</h1>");
		out.println("<p>ERROR: " + message + "</p>");
		out.println("</body></html>");
		out.close();
	}
}
