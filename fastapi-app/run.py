import uvicorn

if __name__ == "__main__":
    """
    Runner script to bootstrap the FastAPI application using Uvicorn.
    
    This is equivalent to Spring Boot's main method in the @SpringBootApplication class,
    which starts the embedded Tomcat container. In Python, we use Uvicorn as our
    high-performance ASGI server, pointing to the 'app' instance in 'app.main'.
    """
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,  # Enables hot-reloading on code changes (similar to spring-boot-devtools)
        log_level="info"
    )
