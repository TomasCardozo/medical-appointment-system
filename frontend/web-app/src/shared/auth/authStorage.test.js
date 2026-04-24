import { clearStoredToken, getStoredToken, setStoredToken } from "./authStorage";

describe("authStorage", () => {
  beforeEach(() => {
    clearStoredToken();
  });

  it("stores and retrieves token", () => {
    setStoredToken("token-123");

    expect(getStoredToken()).toBe("token-123");
  });

  it("clears token", () => {
    setStoredToken("token-123");
    clearStoredToken();

    expect(getStoredToken()).toBeNull();
  });
});
