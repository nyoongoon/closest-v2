package subscription

type Repository interface {
	Save(s *Subscription) (int64, error)
	FindByID(id int64) (*Subscription, error)
	Delete(id int64) error
	Update(s *Subscription) error
	FindAllOrderByVisitCountDesc(page, size int) ([]*Subscription, error)
	FindByMemberEmailOrderByVisitCountDesc(email string, page, size int) ([]*Subscription, error)
	FindByMemberEmailOrderByPublishedDateTimeDesc(email string, page, size int) ([]*Subscription, error)
}
