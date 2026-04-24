import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../auth/AuthContext";
import { apiClient } from "../../../../shared/api/client";
import { getApiErrorMessage } from "../../../../shared/api/errors";

const EMPTY_PASSWORD_FORM = {
  currentPassword: "",
  newPassword: "",
  confirmNewPassword: ""
};

const EMPTY_DOCTOR_FORM = {
  specialty: "",
  licenseNumber: "",
  clinicAddress: "",
  bio: ""
};

function hasText(value) {
  return Boolean(value && value.trim());
}

export function useProfilePage() {
  const { user, refreshMe } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [doctorProfile, setDoctorProfile] = useState(null);

  const [accountForm, setAccountForm] = useState({
    fullName: "",
    email: "",
    role: ""
  });

  const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_FORM);
  const [doctorForm, setDoctorForm] = useState(EMPTY_DOCTOR_FORM);

  const isDoctor = user?.role === "DOCTOR";

  useEffect(() => {
    let alive = true;

    async function bootstrap() {
      if (!user) {
        if (alive) {
          setLoading(false);
        }
        return;
      }

      if (alive) {
        setAccountForm({
          fullName: user.fullName || "",
          email: user.email || "",
          role: user.role || ""
        });
        setError("");
      }

      if (!isDoctor) {
        if (alive) {
          setDoctorProfile(null);
          setDoctorForm(EMPTY_DOCTOR_FORM);
          setLoading(false);
        }
        return;
      }

      if (alive) {
        setLoading(true);
      }

      try {
        const response = await apiClient.get("/doctors/me");
        if (alive) {
          setDoctorProfile(response.data);
          setDoctorForm({
            specialty: response.data.specialty || "",
            licenseNumber: response.data.licenseNumber || "",
            clinicAddress: response.data.clinicAddress || "",
            bio: response.data.bio || ""
          });
        }
      } catch (loadError) {
        if (!alive) {
          return;
        }

        if (loadError?.response?.status === 404) {
          setDoctorProfile(null);
          setDoctorForm(EMPTY_DOCTOR_FORM);
          setError("Doctor profile not found.");
        } else {
          setError(getApiErrorMessage(loadError, "Could not load profile"));
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    bootstrap();

    return () => {
      alive = false;
    };
  }, [isDoctor, user]);

  const hasPasswordChange = useMemo(
    () => hasText(passwordForm.currentPassword) || hasText(passwordForm.newPassword) || hasText(passwordForm.confirmNewPassword),
    [passwordForm.currentPassword, passwordForm.newPassword, passwordForm.confirmNewPassword]
  );

  const handleAccountChange = useCallback((field, value) => {
    setAccountForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handlePasswordChange = useCallback((field, value) => {
    setPasswordForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const handleDoctorChange = useCallback((field, value) => {
    setDoctorForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const validateForm = useCallback(() => {
    if (!hasText(accountForm.fullName)) {
      return "Full name is required";
    }

    if (hasPasswordChange) {
      if (!hasText(passwordForm.currentPassword) || !hasText(passwordForm.newPassword) || !hasText(passwordForm.confirmNewPassword)) {
        return "Current password, new password, and confirmation are required";
      }

      if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
        return "New password and confirmation must match";
      }
    }

    if (isDoctor && (!hasText(doctorForm.specialty) || !hasText(doctorForm.licenseNumber))) {
      return "Specialty and license number are required";
    }

    return null;
  }, [accountForm.fullName, doctorForm.licenseNumber, doctorForm.specialty, hasPasswordChange, isDoctor, passwordForm.confirmNewPassword, passwordForm.currentPassword, passwordForm.newPassword]);

  const resetPasswordForm = useCallback(() => {
    setPasswordForm(EMPTY_PASSWORD_FORM);
  }, []);

  const saveProfile = useCallback(async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    const authPayload = {
      fullName: accountForm.fullName
    };

    if (hasPasswordChange) {
      authPayload.currentPassword = passwordForm.currentPassword;
      authPayload.newPassword = passwordForm.newPassword;
    }

    setSaving(true);

    if (!isDoctor) {
      try {
        await apiClient.put("/auth/me", authPayload);
        await refreshMe();
        resetPasswordForm();
        setMessage(hasPasswordChange ? "Profile and password updated successfully." : "Profile updated successfully.");
      } catch (saveError) {
        setError(getApiErrorMessage(saveError, "Could not update profile"));
      } finally {
        setSaving(false);
      }
      return;
    }

    let baseProfileUpdated = false;
    try {
      await apiClient.put("/auth/me", authPayload);
      baseProfileUpdated = true;

      await apiClient.put("/doctors/me", {
        fullName: accountForm.fullName,
        specialty: doctorForm.specialty,
        licenseNumber: doctorForm.licenseNumber,
        clinicAddress: doctorForm.clinicAddress,
        bio: doctorForm.bio
      });

      await refreshMe();
      resetPasswordForm();
      setMessage(hasPasswordChange ? "Profile and password updated successfully." : "Profile updated successfully.");
    } catch (saveError) {
      if (baseProfileUpdated) {
        try {
          await refreshMe();
        } catch {
          // ignore secondary refresh errors on partial updates
        }
        setError("Base profile updated, but doctor profile could not be saved.");
      } else {
        setError(getApiErrorMessage(saveError, "Could not update profile"));
      }
    } finally {
      setSaving(false);
    }
  }, [accountForm.fullName, doctorForm.bio, doctorForm.clinicAddress, doctorForm.licenseNumber, doctorForm.specialty, hasPasswordChange, isDoctor, passwordForm.currentPassword, passwordForm.newPassword, refreshMe, resetPasswordForm, validateForm]);

  return {
    loading,
    saving,
    error,
    message,
    isDoctor,
    doctorProfile,
    accountForm,
    passwordForm,
    doctorForm,
    handleAccountChange,
    handlePasswordChange,
    handleDoctorChange,
    saveProfile
  };
}
