package ru.practicum.main.exception.model;

public class DataConflictException extends RuntimeException {
    public DataConflictException(String message) {
        super(message);
    }
}