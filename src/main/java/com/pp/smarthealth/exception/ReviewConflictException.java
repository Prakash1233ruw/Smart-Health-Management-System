package com.pp.smarthealth.exception;

public class ReviewConflictException extends RuntimeException {
    public ReviewConflictException() { super("Review status transition is not allowed. Refresh the analysis before reviewing."); }
}
