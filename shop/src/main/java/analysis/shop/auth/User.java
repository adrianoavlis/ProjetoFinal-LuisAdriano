package analysis.shop.auth;

public interface User {
	
    void setId(Long id);
    Long getId();
    
    void setName(String name);
    String getName();
    
    void setEmail(String email);
    String getEmail();
    
    void setPassword(String password);
    String getPassword();
    
    boolean authenticate(String email, String password);
}
