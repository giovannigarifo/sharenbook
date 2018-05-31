package it.polito.mad.sharenbook.model;

public class Review {

    Review(){}

    private Integer rating;
    private String rTitle;
    private String rText;
    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getrTitle() {
        return rTitle;
    }

    public void setrTitle(String rTitle) {
        this.rTitle = rTitle;
    }

    public String getrText() {
        return rText;
    }

    public void setrText(String rText) {
        this.rText = rText;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public Boolean getGiven() {
        return given;
    }

    public void setGiven(Boolean given) {
        this.given = given;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    private String creator;
    private Long date;
    private Boolean given;
    private String bookId;

}