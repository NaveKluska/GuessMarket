<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <title>Hello Spring MVC</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            text-align: center;
        }
        .container {
            padding: 20px;
            border: 1px solid #ccc;
            border-radius: 5px;
            display: inline-block;
            background-color: #f9f9f9;
        }
        h1 {
            color: #4285f4;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Spring MVC - Traditional Style</h1>
        <p>${message}</p>
        <p>This is how web applications were built before Spring Boot!</p>
    </div>
</body>
</html>
