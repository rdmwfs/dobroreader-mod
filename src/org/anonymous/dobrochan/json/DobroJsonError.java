package org.anonymous.dobrochan.json;

public class DobroJsonError {
	@SuppressWarnings("unused")
	private static class Error {
		private String message;
		private int code;

		// private String data;
		public Error(String message, int code) {
			this.setMessage(message);
			this.setCode(code);
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}
	}

	private Error error;

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}
}
