import { getApiErrorMessage } from "./errors";

describe("getApiErrorMessage", () => {
  it("returns API message when present", () => {
    const message = getApiErrorMessage({
      response: { data: { message: "backend message" } }
    });

    expect(message).toBe("backend message");
  });

  it("returns error.message when backend message is missing", () => {
    const message = getApiErrorMessage(new Error("network error"));

    expect(message).toBe("network error");
  });

  it("returns fallback when no details are available", () => {
    const message = getApiErrorMessage({}, "default message");

    expect(message).toBe("default message");
  });
});
