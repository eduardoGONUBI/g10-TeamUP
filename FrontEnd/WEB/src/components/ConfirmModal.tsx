// src/components/ConfirmModal.tsx
import React from "react";
import "./ConfirmModal.css";

export interface ConfirmModalProps {
  isOpen: boolean;
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmModal({
  isOpen,
  title = "Please Confirm",
  message,
  confirmText = "Yes",
  cancelText = "No",
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  if (!isOpen) return null;
  return (
    <div className="cm-overlay" onClick={onCancel}>
      <div className="cm-modal" onClick={e => e.stopPropagation()}>
        <h3 className="cm-title">{title}</h3>
        <p className="cm-message">{message}</p>
        <div className="cm-buttons">
          <button className="cm-btn cm-btn-cancel" onClick={onCancel}>
            {cancelText}
          </button>
          <button className="cm-btn cm-btn-confirm" onClick={onConfirm}>
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
