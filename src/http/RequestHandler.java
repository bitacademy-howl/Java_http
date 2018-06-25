package http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class RequestHandler extends Thread {
	private static final String DOCUMENT_ROOT = "./webapp";

	private Socket socket;

	public RequestHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			// logging Remote Host IP Address & Port
			InetSocketAddress inetSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
			consoleLog("connected from " + inetSocketAddress.getAddress().getHostAddress() + ":"
					+ inetSocketAddress.getPort());
			System.out.println("=============================== Request Information ==================================");
			// get IOStream
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			OutputStream os = socket.getOutputStream();
			
			String request = null;
			
			while(true) {
				
				String line = br.readLine();
				
				if (line==null || "".equals(line)) {
					break;
				}
				if (request == null) {
					request = line;
//					break;
				}

				consoleLog(line);
			}
			System.out.println("======================================================================================");
			consoleLog("Request : " + request);
			System.out.println("======================================================================================");
			
//			파일의 디렉토리명 추출
			String[] tokens = request.split( " " );
			for(int i=0; i < tokens.length; i++) {
				System.err.println(tokens[i]);
			}
//			System.err.println(tokens[1]);
			
			// 400 에러 처리
			if(tokens[0].equals("GET")) {
				
				// 루트 디렉터리 설정
				if (tokens[1].equals("/"))
					tokens[1] = "/index.html";
				// http request 의 첫 라인은 
				responseStaticResource(os, tokens[1], tokens[2]);
			}else {
				response400Error(os, tokens[2]);
			}

		} catch ( Exception ex ) {
			consoleLog( "error:" + ex );
		} finally {
			// clean-up
			try {
				if ( socket != null && socket.isClosed() == false ) {
					socket.close();
				}
			} catch ( IOException ex)  {
				consoleLog( "error:" + ex );
			}
		}
	}

	private void consoleLog(String message) {
		System.out.println("[RequestHandler#" + getId() + "] " + message);
	}
	
	private void responseStaticResource( OutputStream outputStream, String url, String protocol ) throws IOException {
		try {
			
			File file = new File( "./webapp" + url );
			Path path = file.toPath();
			byte[] body = Files.readAllBytes( path );
			
//			마임타입의 추출  - 페이지 소스로부터 얻을 수 있다. (정의 되어 있을 경우)
			String mimeType = Files.probeContentType( path );
			System.out.println("마임타입 정의 (text/css) : " + mimeType);
			
			outputStream.write( "HTTP/1.1 200 OK\r\n".getBytes( "UTF-8" ));
			outputStream.write( ("Content-Type:" + mimeType + "\r\n").getBytes( "UTF-8" ) );
			outputStream.write( "\r\n".getBytes() );
			outputStream.write( body );
			
		}catch (NoSuchFileException nSFE){
			response404Error( outputStream, protocol );
			return;
		}
	}

	private void response404Error(OutputStream outputStream, String protocol) throws IOException {
		
		File file = new File( "./webapp" + "/error/404.html" );
		Path path = file.toPath();
		byte[] body;
		body = Files.readAllBytes( path );
		
		outputStream.write( ( protocol + " 404 File Not Found\r\n" ).getBytes() );
		outputStream.write( "Content-Type:text/html\r\n".getBytes() );
		outputStream.write( "\r\n".getBytes() );
		outputStream.write( body );
	}
	
	private void response400Error(OutputStream outputStream, String protocol) throws Exception{
		File file = new File( "./webapp" + "/error/400.html" );
		Path path = file.toPath();
		byte[] body;
		body = Files.readAllBytes( path );
		
		outputStream.write( ( protocol + " 400 Bad Request\r\n" ).getBytes() );
		outputStream.write( "Content-Type:text/html\r\n".getBytes() );
		outputStream.write( "\r\n".getBytes() ); 
		outputStream.write( body );
	}
}