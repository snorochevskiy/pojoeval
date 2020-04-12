package snorochevskiy.pojoeval.v2.evaluator.pojos;

import java.util.List;

public class Programmer {
    private String firstName; // "John"
    private String lastName; // "Doe"
    private String birthData; // DD MM YYYY
    private String location; // "Office1-Room2", "Office5-Room19"

    private String grade; // "Junior", "Middle", "Senior", etc.
    private String position; // "Software engineer", "Network engineer", "Business analysts"
    private String academicDegree; // "None", "Bachelor", "Master", "PhD"
    private List<String> skills; // ["C++", "Haskell", "Erlang"]

    public Programmer(String firstName, String lastName, String birthData, String location, String grade,
                      String position, String academicDegree, List<String> skills) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthData = birthData;
        this.location = location;
        this.grade = grade;
        this.position = position;
        this.academicDegree = academicDegree;
        this.skills = skills;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getBirthData() { return birthData; }
    public String getLocation() { return location; }
    public String getGrade() { return grade; }
    public String getPosition() { return position; }
    public String getAcademicDegree() { return academicDegree; }
    public List<String> getSkills() { return skills; }
}