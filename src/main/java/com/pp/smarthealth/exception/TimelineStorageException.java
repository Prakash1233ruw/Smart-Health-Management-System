package com.pp.smarthealth.exception;

public class TimelineStorageException extends RuntimeException {
    public TimelineStorageException() { super("Health timeline storage is temporarily unavailable."); }
}
